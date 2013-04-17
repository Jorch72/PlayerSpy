package au.com.mineauz.PlayerSpy.commands;

import java.lang.reflect.Field;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.debugging.Profiler;
import au.com.mineauz.PlayerSpy.structurefile.HoleEntry;
import au.com.mineauz.PlayerSpy.tracdata.FileHeader;
import au.com.mineauz.PlayerSpy.tracdata.HoleIndex;
import au.com.mineauz.PlayerSpy.tracdata.RollbackEntry;
import au.com.mineauz.PlayerSpy.tracdata.RollbackIndex;
import au.com.mineauz.PlayerSpy.tracdata.SessionEntry;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;
import au.com.mineauz.PlayerSpy.tracdata.LogFileRegistry;

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
		return "playerspy.inspect";
	}

	@Override
	public String[] getUsageString(String label, CommandSender sender) 
	{
		return new String[] {label + "(level|analyse <logname>|log|resetlog)"};
	}

	@Override
	public String getDescription()
	{
		return "Debug command";
	}
	
	@Override
	public boolean canBeConsole() 
	{
		return true;
	}
	@Override
	public boolean canBeCommandBlock()
	{
		return false;
	}

	private void analyseLog(CommandSender sender, String logName, String focus)
	{
		LogFile log = LogFileRegistry.getLogFile(Bukkit.getOfflinePlayer(logName));
		
		if(log == null)
		{
			sender.sendMessage("Unable to find log " + logName);
			return;
		}

		int totalCount = 0;
		sender.sendMessage(ChatColor.GREEN + "Totalling the log. Please wait.");
		for(SessionEntry session : log.getSessions())
		{
			String tag = log.getOwnerTag(session);
			if(session.Compressed)
				sender.sendMessage((tag != null ? tag : " Session " + session.Id) + "(C): Total Size: " + session.TotalSize);
			else
				sender.sendMessage((tag != null ? tag : " Session " + session.Id) + ": Total Size: " + session.TotalSize);
		}
		
		sender.sendMessage(ChatColor.GREEN + "Results ( " + totalCount + " total ): ");
		
		HoleIndex holeIndex;
		RollbackIndex rollbackIndex;
		FileHeader header;
		
		try
		{
			Field field = log.getClass().getDeclaredField("mHoleIndex");
			field.setAccessible(true);
			
			holeIndex = (HoleIndex) field.get(log);
			
			field = log.getClass().getDeclaredField("mHeader");
			field.setAccessible(true);
			
			header = (FileHeader)field.get(log);
			
			field = log.getClass().getDeclaredField("mRollbackIndex");
			field.setAccessible(true);
			
			rollbackIndex = (RollbackIndex) field.get(log);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return;
		}

		long totalFreeSpace = 0;
		long totalReservedSpace = 0;
		
		sender.sendMessage(ChatColor.GREEN + "Layout:");
		TreeMap<Long,Pair<Long,Object>> sortedItems = new TreeMap<Long, Pair<Long,Object>>();
		sortedItems.put(0L, new Pair<Long,Object>((long)header.getSize(),"Header"));
		
		sortedItems.put(header.IndexLocation, new Pair<Long,Object>(header.IndexSize,"Session Index"));
		sortedItems.put(header.HolesIndexLocation, new Pair<Long,Object>(header.HolesIndexSize,"Holes Index (" + header.HolesIndexPadding + ")"));
		sortedItems.put(header.OwnerMapLocation, new Pair<Long,Object>(header.OwnerMapSize,"OwnerMap"));
		sortedItems.put(header.RollbackIndexLocation, new Pair<Long,Object>(header.RollbackIndexSize,"RollbackIndex"));
		
		for(HoleEntry hole : holeIndex)
		{
			
			//sender.sendMessage(" loc:" + hole.Location + " size:" + hole.Size + " attached to:" + (hole.AttachedTo == null ? "none" : hole.AttachedTo.Id));
			sortedItems.put(hole.Location, new Pair<Long,Object>(hole.Size,"Hole"));
			totalFreeSpace += hole.Size;
		}
		
		for(RollbackEntry entry : rollbackIndex)
		{
			sortedItems.put(entry.detailLocation, new Pair<Long,Object>(entry.detailSize,"RollbackDetail for Session " + entry.sessionId));
		}
		
		for(SessionEntry session : log.getSessions())
		{
			String tag = log.getOwnerTag(session);
			
			sortedItems.put(session.Location, new Pair<Long,Object>(session.TotalSize,"Session " + (tag != null ? tag + "(" + session.Id + ")" : session.Id) + " Padding: " + session.Padding));
		}
		
		// Find any Unallocated space
		TreeMap<Long, Pair<Long, Object>> extra = new TreeMap<Long, Pair<Long, Object>>();
		
		long lastPos = 0;
		String last = "";
		for(Entry<Long, Pair<Long, Object>> entry : sortedItems.entrySet())
		{
			if(lastPos > entry.getKey())
				extra.put(entry.getKey()+1, new Pair<Long,Object>(lastPos - entry.getKey(), ChatColor.RED + "CONFLICT!! " + entry.getKey() + " -> " + (entry.getKey() + entry.getValue().getArg1()) + " with " + last));
			else if(lastPos < entry.getKey())
			{
				extra.put(lastPos, new Pair<Long,Object>(entry.getKey() - lastPos, ChatColor.RED + "Unallocated space!"));
				lastPos = entry.getKey() + entry.getValue().getArg1();
				last = (String)entry.getValue().getArg2();
			}
			else
			{
				lastPos = entry.getKey() + entry.getValue().getArg1();
				last = (String)entry.getValue().getArg2();
			}
		}
		sortedItems.putAll(extra);
		
		for(Entry<Long, Pair<Long, Object>> entry : sortedItems.entrySet())
		{
			sender.sendMessage(entry.getValue().getArg2() + ": loc=" + entry.getKey() + " size=" + entry.getValue().getArg1());
		}
		
		sender.sendMessage(ChatColor.GREEN + "Total Free Space: " + ChatColor.RESET + totalFreeSpace);
		sender.sendMessage(ChatColor.GREEN + "Total Reserved Space: " + ChatColor.RESET + totalReservedSpace);
		
		LogFileRegistry.unloadLogFile(Bukkit.getOfflinePlayer(logName));
	}
	
	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length == 0)
			return false;
		
		if(args[0].equalsIgnoreCase("level"))
		{
			if(args.length != 2)
				return false;
			
			if(!(sender instanceof Player))
				return false;
			
			Level level = null;
			
			try
			{
				level = Level.parse(args[1].toUpperCase());
			}
			catch(IllegalArgumentException e)
			{
				return false;
			}
			
			Debug.setDebugLevel((Player)sender, level);
			
			sender.sendMessage("Debug Level " + level.toString());
		}
		else if(args[0].equalsIgnoreCase("analyse"))
		{
			if(args.length > 3)
				return false;
			
			String logName = args[1];
			String focus = (args.length > 2 ? args[2] : null);
			analyseLog(sender, logName, focus);
		}
		else if(args[0].equalsIgnoreCase("log"))
		{
			if(args.length > 1)
				return false;
			
			Profiler.outputDebugData();
			sender.sendMessage("Saved debug log");
		}
		else if(args[0].equalsIgnoreCase("resetlog"))
		{
			if(args.length > 1)
				return false;
			
			Debug.clearLog();
			sender.sendMessage("Cleared log");
		}
		else if(args[0].equalsIgnoreCase("test"))
		{

		}
		else if(args[0].equalsIgnoreCase("test2"))
		{
			Random r = new Random(123456778);
			
			BitSet set = new BitSet(256);
			
			for(int i = 0; i < 1000; ++i)
			{
				long hash = (r.nextInt(20000) - 10000);
				
				hash = ((hash * 39) ^ hash) ^ (hash * 11);
				
				long ohash = hash;
				hash = 0;
				
				for(int b = 0; b < 4; ++b)
				{
					set.set((int)Math.abs((ohash % set.size())));
					ohash ^= (ohash * 13);
				}
			}
			
			sender.sendMessage(Utility.bitSetToString(set));
		}
		else
			return false;
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
