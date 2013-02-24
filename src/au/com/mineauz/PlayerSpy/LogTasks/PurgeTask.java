package au.com.mineauz.PlayerSpy.LogTasks;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;
import au.com.mineauz.PlayerSpy.tracdata.LogFileRegistry;


public class PurgeTask  implements Task<Boolean>
{
	private final String mLogOwner;
	private final boolean mAllLogs;
	private final long mFromDate;
	private final long mToDate;
	private final CommandSender mCaller;
	public PurgeTask(CommandSender caller, String owner, boolean all, long from, long to)
	{
		mLogOwner = owner;
		mAllLogs = all;
		mFromDate = from;
		mToDate = to;
		mCaller = caller;
	}
	@Override
	public Boolean call() throws Exception 
	{
		Debug.finer("Starting PurgeTask");
		if(mAllLogs)
		{
			mCaller.sendMessage(ChatColor.GOLD + "Starting Purge from all logs.");
			for(File file : LogFileRegistry.getLogFileDirectory().listFiles())
			{
				if(!file.getName().endsWith(LogFileRegistry.cFileExt))
					continue;
				
				String name = file.getName().substring(0, file.getName().indexOf(LogFileRegistry.cFileExt));
				LogFile log = null;
				
				if(name.startsWith(LogFileRegistry.cGlobalFilePrefix))
					log = LogFileRegistry.getLogFile(Bukkit.getWorld(name.substring(LogFileRegistry.cGlobalFilePrefix.length())));
				else
					log = LogFileRegistry.getLogFile(Bukkit.getOfflinePlayer(name));
				
				purge(log);
			}
		}
		else
		{
			LogFile log = null;
			
			if(mLogOwner.startsWith(LogFileRegistry.cGlobalFilePrefix))
				log = LogFileRegistry.getLogFile(Bukkit.getWorld(mLogOwner.substring(LogFileRegistry.cGlobalFilePrefix.length())));
			else
				log = LogFileRegistry.getLogFile(Bukkit.getOfflinePlayer(mLogOwner));
			
			purge(log);
		}
		
		mCaller.sendMessage(ChatColor.GOLD + "Purge Complete.");
		
		return true;
	}
	private void purge(LogFile log)
	{
		if(log == null)
			return;
		
		if(mFromDate < log.getStartDate() && mToDate > log.getEndDate())
		{
			// purge the whole file
			mCaller.sendMessage(ChatColor.GOLD + " Purging " + log.getName());
			
			log.close(true);
			
			// If its still loaded we cant just delete the whole file
			if(log.isLoaded())
			{
				if(!log.purgeRecords(mFromDate,mToDate))
					mCaller.sendMessage(ChatColor.RED + "  Purge failed on " + log.getName());
			}
			else
			{
				if(!log.getFile().delete())
					mCaller.sendMessage(ChatColor.RED + "  Purge failed on " + log.getName());
				else
				{
					CrossReferenceIndex.instance.removeLogFile(log);
				}
			}
		}
		else
		{
			mCaller.sendMessage(ChatColor.GOLD + " Purging " + log.getName() + " from " + Utility.formatTime(mFromDate, "dd/MM/yy HH:mm:ss") + " to " + Utility.formatTime(mToDate, "dd/MM/yy HH:mm:ss"));
			
			if(!log.purgeRecords(mFromDate,mToDate))
				mCaller.sendMessage(ChatColor.RED + "  Purge failed on " + log.getName());

			log.close(false);
		}
	}
	@Override
	public int getTaskTargetId()
	{
		return 999923451;
	}
	


}
