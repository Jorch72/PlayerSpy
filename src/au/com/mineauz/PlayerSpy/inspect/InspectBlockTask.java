package au.com.mineauz.PlayerSpy.inspect;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.Pair;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.SafeChunk;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Utility;
import au.com.mineauz.PlayerSpy.LogTasks.Task;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.InteractRecord;
import au.com.mineauz.PlayerSpy.Records.InventoryTransactionRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;
import au.com.mineauz.PlayerSpy.monitoring.ShallowMonitor;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex.SessionInFile;

public class InspectBlockTask implements Task<Void>
{
	private Player mWho;
	private Location mLocation;
	private ArrayList<Pair<Cause, Record>> mostRecent;
	
	public InspectBlockTask(Player who, Location block)
	{
		mWho = who;
		mLocation = block.clone();
	}
	
	private void processRecords(Cause cause, RecordList list)
	{
		if(mostRecent.size() < SpyPlugin.getSettings().inspectCount || list.getEndTimestamp() > mostRecent.get(mostRecent.size()-1).getArg2().getTimestamp())
		{
			long minDate = 0;
			if(mostRecent.size() >= SpyPlugin.getSettings().inspectCount)
				minDate = mostRecent.get(mostRecent.size()-1).getArg2().getTimestamp();
			
			ListIterator<Record> it = list.listIterator(list.size()-1);
			// Find all block change records at that location
			while(it.hasPrevious())
			{
				Record record = it.previous();
				
				if(record.getTimestamp() < minDate)
					break;
				
				if(processRecord(cause, record))
				{
					// It was inserted. Recheck whether we need to search or not
					if(mostRecent.size() >= SpyPlugin.getSettings().inspectCount)
						break;
					minDate = Math.min(mostRecent.get(mostRecent.size()-1).getArg2().getTimestamp(),minDate);
				}
			}
		}
	}
	private boolean processRecord(Cause cause, Record record)
	{
		if(record.getType() == RecordType.BlockChange)
		{
			if(((BlockChangeRecord)record).getLocation().equals(mLocation))
			{
				insertRecord(cause, record);
				return true;
			}
		}
		else if(record.getType() == RecordType.ItemTransaction && SpyPlugin.getSettings().inspectTransactions)
		{
			InventoryTransactionRecord transaction = (InventoryTransactionRecord)record;
			if(transaction.getInventoryInfo().getBlock() != null && transaction.getInventoryInfo().getBlock().getLocation().equals(mLocation))
			{
				insertRecord(cause, record);
				return true;
			}
		}
		else if(record.getType() == RecordType.Interact && SpyPlugin.getSettings().inspectUse)
		{
			InteractRecord interact = (InteractRecord)record;
			if(interact.hasBlock() && interact.getBlock().getLocation().equals(mLocation))
			{
				insertRecord(cause, record);
				return true;
			}
		}
		return false;
	}
	private void insertRecord(Cause cause, Record record)
	{
		for(int i = 0; i < mostRecent.size(); i++)
		{
			if(record.getTimestamp() > mostRecent.get(i).getArg2().getTimestamp())
			{
				mostRecent.add(i,new Pair<Cause, Record>(cause, record));
				return;
			}
		}
		mostRecent.add(new Pair<Cause, Record>(cause, record));
		
		if(mostRecent.size() > SpyPlugin.getSettings().inspectCount)
			mostRecent.remove(mostRecent.size()-1);
	}
	@Override
	public Void call() 
	{
		mostRecent = new ArrayList<Pair<Cause, Record>>();

		// Add in any records that are yet to be written to file
		for(ShallowMonitor mon : GlobalMonitor.instance.getAllMonitors())
		{
			List<Pair<String, RecordList>> inBuffer = mon.getBufferedRecords();
			for(Pair<String, RecordList> pair : inBuffer)
			{
				Cause cause;
				if(pair.getArg1() != null)
					cause = Cause.playerCause(mon.getMonitorTarget(), pair.getArg1());
				else
					cause = Cause.playerCause(mon.getMonitorTarget());
				
				// Load up the records in the session
				RecordList source = pair.getArg2();

				processRecords(cause, source);
			}
		}
		
		
		// Check stuff saved to disk
		CrossReferenceIndex.Results allSessions = CrossReferenceIndex.instance.getSessionsFor(new SafeChunk(mLocation));
		for(SessionInFile fileSession : allSessions.foundSessions)
		{
			// Dont check ones that clearly have nothing of interest 
			if(mostRecent.size() >= SpyPlugin.getSettings().inspectCount && fileSession.Session.EndTimestamp < mostRecent.get(mostRecent.size()-1).getArg2().getTimestamp())
				continue;
			
			Cause cause;
			String ownerTag = fileSession.Log.getOwnerTag(fileSession.Session);
			if(fileSession.Log.getName().startsWith(LogFileRegistry.cGlobalFilePrefix))
			{
				if(ownerTag == null)
					cause = Cause.unknownCause();
				else
					cause = Cause.globalCause(Bukkit.getWorld(fileSession.Log.getName().substring(LogFileRegistry.cGlobalFilePrefix.length())), ownerTag);
			}
			else
			{
				if(ownerTag == null)
					cause = Cause.playerCause(Bukkit.getOfflinePlayer(fileSession.Log.getName()));
				else
					cause = Cause.playerCause(Bukkit.getOfflinePlayer(fileSession.Log.getName()), ownerTag);
			}
			
			RecordList source = fileSession.Log.loadSession(fileSession.Session);
			
			processRecords(cause, source);
		}
		allSessions.release();
		
		// Format the results into a neat list and display
		ArrayList<String> output = new ArrayList<String>();
		
		long lastDate = 0;
		output.add(ChatColor.GOLD + "[PlayerSpy] " + ChatColor.WHITE + "Block changes " + Utility.locationToStringShort(mLocation));
		if(mostRecent.size() == 0)
			output.add(ChatColor.GREEN + "  No changes to the block detected");
		else
		{
			for(int i = 0; i < mostRecent.size(); i++)
			{
				String msg = mostRecent.get(i).getArg2().getDescription();
				if(msg == null)
					continue;
				
				long date = mostRecent.get(i).getArg2().getTimestamp();
				long dateOnly = Utility.getDatePortion(date);
				date = Utility.getTimePortion(date);
				
				// Output the date if it has changed
				if(lastDate != dateOnly)
				{
					if(dateOnly == Utility.getDatePortion(System.currentTimeMillis()))
						output.add(" " + ChatColor.GREEN + "Today");
					else
					{
						DateFormat fmt = DateFormat.getDateInstance(DateFormat.FULL);
						fmt.setTimeZone(SpyPlugin.getSettings().timezone);
						output.add(" " + ChatColor.GREEN + fmt.format(new Date(dateOnly)));
					}
					lastDate = dateOnly;
				}
				
				output.add(String.format(ChatColor.GREEN + "  %7s " + ChatColor.RESET, Utility.formatTime(date, "hh:mma")) + String.format(msg, ChatColor.RED + mostRecent.get(i).getArg1().friendlyName() + ChatColor.RESET));
				
			}
		}
		
		// Send the message
		for(String line : output)
		{
			mWho.sendMessage(line);
		}
		return null;
	}
	@Override
	public int getTaskTargetId() 
	{
		return 99999999;
	}

}
