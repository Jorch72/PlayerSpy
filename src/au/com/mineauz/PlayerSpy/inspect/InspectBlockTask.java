package au.com.mineauz.PlayerSpy.inspect;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.LogTasks.Task;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.ILocationAware;
import au.com.mineauz.PlayerSpy.Records.IPlayerLocationAware;
import au.com.mineauz.PlayerSpy.Records.IRollbackable;
import au.com.mineauz.PlayerSpy.Records.InteractRecord;
import au.com.mineauz.PlayerSpy.Records.InventoryTransactionRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.Utilities.SafeChunk;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;
import au.com.mineauz.PlayerSpy.monitoring.ShallowMonitor;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex.SessionInFile;
import au.com.mineauz.PlayerSpy.storage.StoredBlock;
import au.com.mineauz.PlayerSpy.storage.StoredEntity;

public class InspectBlockTask implements Task<Void>
{
	private Player mWho;
	private Location mLocation;
	private Location mOffsetLocation;
	private Location mAltLocation;
	private Material mAltType;
	private InspectInfo mSettings;
	
	private ArrayList<Pair<Cause, Record>> mostRecent;
	
	public InspectBlockTask(Player who, Location block, Location altLocation, Material altType, InspectInfo settings)
	{
		mWho = who;
		mLocation = block.clone();
		mOffsetLocation = mLocation.clone().add(0.5, 0.5, 0.5);
		
		mAltLocation = (altLocation != null ? altLocation.clone() : null);
		mAltType = altType;
		
		mSettings = settings;
	}
	
	private void processRecords(Cause cause, RecordList list)
	{
		if(list.size() == 0)
			return;
		
		if(mostRecent.size() < mSettings.itemCount || list.getEndTimestamp() > mostRecent.get(mostRecent.size()-1).getArg2().getTimestamp())
		{
			long minDate = 0;
			if(mostRecent.size() >= mSettings.itemCount)
				minDate = mostRecent.get(mostRecent.size()-1).getArg2().getTimestamp();
			
			ListIterator<Record> it = list.listIterator(list.size()-1);
			// Find all block change records at that location
			while(it.hasPrevious())
			{
				Record record = it.previous();
				
				if(record.getTimestamp() < minDate)
					break;
				
				if(processRecord(cause, record))
					minDate = Math.min(mostRecent.get(mostRecent.size()-1).getArg2().getTimestamp(),minDate);
			}
		}
	}
	private boolean processRecord(Cause cause, Record record)
	{
		if(record.getType() == RecordType.BlockChange)
		{
			if(((BlockChangeRecord)record).getLocation().equals(mLocation) || (((BlockChangeRecord)record).getBlock().getType() == mAltType && ((BlockChangeRecord)record).getLocation().equals(mAltLocation)))
			{
				insertRecord(cause, record);
				return true;
			}
		}
		else if(record.getType() == RecordType.ItemTransaction && mSettings.showItems)
		{
			InventoryTransactionRecord transaction = (InventoryTransactionRecord)record;
			StoredBlock block = transaction.getInventoryInfo().getBlock();
			StoredEntity entity = transaction.getInventoryInfo().getEntity();
			
			if(block != null && (block.getLocation().equals(mLocation) || (block.getType() == mAltType && block.getLocation().equals(mAltLocation))))
			{
				insertRecord(cause, record);
				return true;
			}
			else if(entity != null && entity.getLocation().getWorld() == mLocation.getWorld() &&  entity.getLocation().distanceSquared(mOffsetLocation) < 1.1)
			{
				insertRecord(cause, record);
				return true;
			}
		}
		else if(record.getType() == RecordType.Interact && mSettings.showUse)
		{
			InteractRecord interact = (InteractRecord)record;
			StoredBlock block = interact.getBlock();
			
			if(interact.hasBlock() && (block.getLocation().equals(mLocation) || (block.getType() == mAltType && block.getLocation().equals(mAltLocation))))
			{
				insertRecord(cause, record);
				return true;
			}
		}
		else if(record instanceof ILocationAware && !(record instanceof IPlayerLocationAware) && mSettings.showEntities && (record.getType() != RecordType.ItemTransaction && record.getType() != RecordType.Interact))
		{
			Location location = ((ILocationAware)record).getLocation();
			if(location != null && location.getWorld() == mLocation.getWorld() && location.distanceSquared(mOffsetLocation) < 1.1)
			{
				insertRecord(cause, record);
				return true;
			}
		}
		return false;
	}
	private void insertRecord(Cause cause, Record record)
	{
		boolean added = false;
		for(int i = 0; i < mostRecent.size(); i++)
		{
			if(record.getTimestamp() > mostRecent.get(i).getArg2().getTimestamp())
			{
				mostRecent.add(i,new Pair<Cause, Record>(cause, record));
				added = true;
				break;
			}
		}
		
		if(!added)
			mostRecent.add(new Pair<Cause, Record>(cause, record));
		
		if(mostRecent.size() > mSettings.itemCount)
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
		
		// Global records
		for(World world : Bukkit.getWorlds())
		{
			HashMap<String, RecordList> buffers = GlobalMonitor.instance.getBufferForWorld(world);
			for(Entry<String, RecordList> buffer : buffers.entrySet())
			{
				Cause cause = Cause.globalCause(world, buffer.getKey());
				
				// Load up the records in the session
				processRecords(cause, buffer.getValue());
			}
		}
		
		// Pending records
		for(Pair<RecordList,Cause> pending : GlobalMonitor.instance.getPendingRecords().values())
		{
			// Load up the records in the session
			processRecords(pending.getArg2(), pending.getArg1());
		}
		
		// Check stuff saved to disk
		CrossReferenceIndex.Results allSessions = CrossReferenceIndex.instance.getSessionsFor(new SafeChunk(mLocation));
		for(SessionInFile fileSession : allSessions.foundSessions)
		{
			// Dont check ones that clearly have nothing of interest 
			if(mostRecent.size() >= mSettings.itemCount && fileSession.Session.EndTimestamp < mostRecent.get(mostRecent.size()-1).getArg2().getTimestamp())
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
			if(source.isEmpty())
				continue;
			
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
				
				boolean strike = false;
				if(mostRecent.get(i).getArg2() instanceof IRollbackable)
				{
					if(((IRollbackable)mostRecent.get(i).getArg2()).wasRolledBack())
					{
						strike = true;
					}
				}
				
				String outputStr = String.format(ChatColor.GREEN + "  %7s " + ChatColor.RESET, Utility.formatTime(date, "hh:mma")) + String.format(msg, ChatColor.RED + mostRecent.get(i).getArg1().friendlyName() + ChatColor.RESET);
				if(strike)
				{
					boolean col = false;
					for(int c = 0; c < outputStr.length(); ++c)
					{
						if(outputStr.charAt(c) == ChatColor.COLOR_CHAR)
						{
							col = true;
							++c;
						}
						else if(col)
						{
							outputStr = outputStr.substring(0, c) + ChatColor.STRIKETHROUGH + outputStr.substring(c);
							c += 2;
							col = false;
						}
					}
				}
				output.add(outputStr);
				
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
