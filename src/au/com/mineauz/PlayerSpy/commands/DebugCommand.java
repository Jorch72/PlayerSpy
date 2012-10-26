package au.com.mineauz.PlayerSpy.commands;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.FileHeader;
import au.com.mineauz.PlayerSpy.HoleEntry;
import au.com.mineauz.PlayerSpy.IndexEntry;
import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Records.UpdateInventoryRecord;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;


public class DebugCommand implements ICommand
{

	@Override
	public String getName() 
	{
		return "debug";
	}

	@Override
	public String[] getAliases() 
	{
		return null;
	}

	@Override
	public String getPermission() 
	{
		return null;
	}

	@Override
	public String getUsageString(String label) 
	{
		return label + " <logname>";
	}

	@Override
	public boolean canBeConsole() 
	{
		return true;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length != 1 && args.length != 2)
			return false;
		
		String logName = args[0];
		
		LogFile log = LogFileRegistry.getLogFile(Bukkit.getOfflinePlayer(logName));
		
		if(log == null)
		{
			sender.sendMessage("Unable to find log " + logName);
			return true;
		}
		
		RecordType detailType = null;
		if(args.length >= 2)
		{
			String type = args[1];
			
			detailType = RecordType.valueOf(type);
			if(detailType == null)
			{
				sender.sendMessage("Invalid recordtype: " + type);
				return true;
			}
		}
		
		HashMap<RecordType, Integer> mRecordTypeCounts = new HashMap<RecordType, Integer>();
		HashMap<RecordType, Integer> mRecordTypeSizes = new HashMap<RecordType, Integer>();
		HashMap<Object, Object> mDetailInfo = new HashMap<Object, Object>();
		HashMap<String, Integer> mOwnerInfo = new HashMap<String, Integer>();
		
		int totalCount = 0;
		sender.sendMessage(ChatColor.GREEN + "Totalling the log. Please wait.");
		for(IndexEntry session : log.getSessions())
		{
			RecordList records = log.loadSession(session);
			String tag = log.getOwnerTag(session);
			boolean absolute = tag!=null;
			if(session.Compressed)
				sender.sendMessage((tag != null ? tag : " Session " + session.Id) + "(C): Total Size: " + session.TotalSize + " Total Size Uncompressed: " + records.getDataSize(absolute));
			else
				sender.sendMessage((tag != null ? tag : " Session " + session.Id) + ": Total Size: " + session.TotalSize);
			if(records != null)
			{
				for(Record record : records)
				{
					if(mRecordTypeCounts.containsKey(record.getType()))
					{
						mRecordTypeCounts.put(record.getType(), mRecordTypeCounts.get(record.getType()) + 1);
						mRecordTypeSizes.put(record.getType(), mRecordTypeSizes.get(record.getType()) + record.getSize(absolute));
					}
					else
					{
						mRecordTypeCounts.put(record.getType(), 1);
						mRecordTypeSizes.put(record.getType(), record.getSize(absolute));
					}
					
					if(record.getType() == detailType)
					{
						String owner = log.getOwnerTag(session);
						
						if(mOwnerInfo.containsKey(owner))
							mOwnerInfo.put(owner,mOwnerInfo.get(owner) + 1);
						else
							mOwnerInfo.put(owner, 1);
						
						if(detailType == RecordType.BlockChange)
						{
							if(mDetailInfo.containsKey(((BlockChangeRecord)record).getBlock().getType()))
								mDetailInfo.put(((BlockChangeRecord)record).getBlock().getType(), (Integer)mDetailInfo.get(((BlockChangeRecord)record).getBlock().getType()) + 1);
							else
								mDetailInfo.put(((BlockChangeRecord)record).getBlock().getType(), 1);
						}
						else if(detailType == RecordType.UpdateInventory)
						{
							Integer count = ((UpdateInventoryRecord)record).Slots.size();
							
							if(mDetailInfo.containsKey(count))
								mDetailInfo.put(count, (Integer)mDetailInfo.get(count) + 1);
							else
								mDetailInfo.put(count, 1);
						}
					}
					totalCount++;
				}
			}
		}
		
		LogFileRegistry.unloadLogFile(Bukkit.getOfflinePlayer(logName));
		
		sender.sendMessage(ChatColor.GREEN + "Results ( " + totalCount + " total ): ");
		if(detailType == null)
		{
			for(Entry<RecordType, Integer> entry : mRecordTypeCounts.entrySet())
			{
				sender.sendMessage(" " + entry.getKey().toString() + ": " + entry.getValue() + " = " + mRecordTypeSizes.get(entry.getKey()) + "bytes");
			}
		}
		else if(mRecordTypeCounts.containsKey(detailType))
		{
			sender.sendMessage(" " + detailType.toString() + ": " + mRecordTypeCounts.get(detailType) + " = " + mRecordTypeSizes.get(detailType) + "bytes");
			
			for(Entry<Object,Object> entry : mDetailInfo.entrySet())
			{
				sender.sendMessage("  " + entry.getKey() + ": " + entry.getValue());
			}
			
			sender.sendMessage(ChatColor.GREEN + "Owner Counts:");
			
			for(Entry<String,Integer> entry : mOwnerInfo.entrySet())
			{
				sender.sendMessage("  " + entry.getKey() + ": " + entry.getValue());
			}
		}
		
		ArrayList<HoleEntry> holeIndex;
		FileHeader header;
		
		try
		{
			Field field = log.getClass().getDeclaredField("mHoleIndex");
			field.setAccessible(true);
			
			holeIndex = (ArrayList<HoleEntry>) field.get(log);
			
			field = log.getClass().getDeclaredField("mHeader");
			field.setAccessible(true);
			
			header = (FileHeader)field.get(log);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return true;
		}

		long totalFreeSpace = 0;
		long totalReservedSpace = 0;
		
		sender.sendMessage(ChatColor.GREEN + "Layout:");
		TreeMap<Long,Pair<Long,Object>> sortedItems = new TreeMap<Long, Pair<Long,Object>>();
		sortedItems.put(0L, new Pair<Long,Object>((long)header.getSize(),"Header"));
		
		sortedItems.put(header.IndexLocation, new Pair<Long,Object>(header.IndexSize,"Index"));
		sortedItems.put(header.HolesIndexLocation, new Pair<Long,Object>(header.HolesIndexSize + header.HolesIndexPadding,"Holes Index (" + header.HolesIndexPadding + ")"));
		sortedItems.put(header.OwnerMapLocation, new Pair<Long,Object>(header.OwnerMapSize,"OwnerMap"));
		
		for(HoleEntry hole : holeIndex)
		{
			sortedItems.put(hole.Location, new Pair<Long,Object>(hole.Size,"Hole"));
			//sender.sendMessage(" loc:" + hole.Location + " size:" + hole.Size + " attached to:" + (hole.AttachedTo == null ? "none" : hole.AttachedTo.Id));
			if(hole.AttachedTo != null)
				totalReservedSpace += hole.Size;
			else
				totalFreeSpace += hole.Size;
		}
		
		for(IndexEntry session : log.getSessions())
		{
			String tag = log.getOwnerTag(session);
			
			sortedItems.put(session.Location, new Pair<Long,Object>(session.TotalSize,"Session " + (tag != null ? tag : session.Id)));
		}
		
		for(Entry<Long, Pair<Long, Object>> entry : sortedItems.entrySet())
		{
			sender.sendMessage(entry.getValue().getArg2() + ": loc=" + entry.getKey() + " size=" + entry.getValue().getArg1());
		}
		
		sender.sendMessage(ChatColor.GREEN + "Total Free Space: " + ChatColor.RESET + totalFreeSpace);
		sender.sendMessage(ChatColor.GREEN + "Total Reserved Space: " + ChatColor.RESET + totalReservedSpace);
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
