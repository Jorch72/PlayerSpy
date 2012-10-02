package au.com.mineauz.PlayerSpy.monitoring;

import java.io.File;
import java.util.HashMap;

import org.bukkit.OfflinePlayer;

import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;

public class LogFileRegistry 
{
	private static File mLogsRoot;
	public static final String cFileExt = ".trackdata";
	private static final String cGlobalFileName = "__global";
	
	private static HashMap<OfflinePlayer, LogFile> mLoadedLogs = new HashMap<OfflinePlayer, LogFile>();
	
	private static LogFile mGlobalLog = null;
	/**
	 * Sets the directory that the logs are stored in
	 */
	public static void setLogFileDirectory(File directory)
	{
		mLogsRoot = directory;
	}
	/**
	 * Gets the log file directory
	 */
	public static File getLogFileDirectory()
	{
		return mLogsRoot;
	}
	/**
	 * Gets a loaded log file for the player, or loads one. It does NOT create one if it doesnt exist
	 * Calling this will add a reference onto it so make sure to close it when done
	 * @return null if it didnt exist.
	 */
	public static LogFile getLogFile(OfflinePlayer player)
	{
		if(mLoadedLogs.containsKey(player))
		{
			LogFile log = mLoadedLogs.get(player);
			log.addReference();
			return log;
		}
		
		File file = new File(mLogsRoot, sanitiseName(player.getName()) + cFileExt);
		if(!file.exists())
			return null;
		
		LogFile log = new LogFile();
		if(!log.load(file.getAbsolutePath()))
		{
			LogUtil.warning("Player log for " + player.getName() + " failed to load. Corrupted?");
			return null;
		}
		
		mLoadedLogs.put(player, log);
		return log;
	}
	
	/**
	 * Creates a log file for the player. If it already exists, this will fail
	 * Calling this will add a reference onto it so make sure to close it when done
	 * @return null if it already exists
	 */
	public static LogFile createLogFile(OfflinePlayer player)
	{
		File file = new File(mLogsRoot, sanitiseName(player.getName()) + cFileExt);
		if(file.exists())
			return null;
		
		LogFile log = LogFile.create(player.getName(), file.getAbsolutePath());
		
		if(log == null)
			return null;
		
		mLoadedLogs.put(player, log);
		return log;
	}
	
	/**
	 * Gets the global log or loads it. It does NOT create it if it doesnt exist
	 * Calling this will add a reference onto it so make sure to close it when done
	 * @return null if it didnt exist.
	 */
	public static LogFile getGlobalLog()
	{
		if(mGlobalLog != null)
		{
			mGlobalLog.addReference();
			return mGlobalLog;
		}
		
		File file = new File(mLogsRoot, cGlobalFileName + cFileExt);
		if(!file.exists())
			return null;
		
		LogFile log = new LogFile();
		if(!log.load(file.getAbsolutePath()))
			return null;
		
		mGlobalLog = log;
		
		return log;
	}
	
	/**
	 * Creates the global log. If it already exists, this will fail
	 * Calling this will add a reference onto it so make sure to close it when done
	 * @return null if it already exists
	 */
	public static LogFile createGlobalLog()
	{
		File file = new File(mLogsRoot, cGlobalFileName + cFileExt);
		if(file.exists())
			return null;
		
		LogFile log = LogFile.createGlobal(file.getAbsolutePath());
		
		if(log == null)
			return null;
		
		mGlobalLog = log;
		return log;
	}
	/**
	 * Unloads a log loaded with getLogFile() or createLogFile()
	 * @return True if the log has been unloaded. False if the log wasnt loaded
	 */
	public static boolean unloadLogFile(OfflinePlayer player)
	{
		LogFile log = mLoadedLogs.get(player);
		
		if(log == null)
			return false;
		
		// Check for bad behaviour
		if(log.isLoaded())
		{
			LogUtil.fine("WARNING: Log was not closed correctly before unloadLogFile(). Forcing closed");
			while(log.isLoaded())
				log.close();
		}
		
		return mLoadedLogs.remove(player) != null;
	}
	public static boolean unloadGlobalLogFile() 
	{
		if(mGlobalLog == null)
			return false;
		
		// Check for bad behaviour
		if(mGlobalLog.isLoaded())
		{
			LogUtil.fine("WARNING: Global Log was not closed correctly before unloadGlobalLogFile(). Forcing closed");
			while(mGlobalLog.isLoaded())
				mGlobalLog.close();
		}
		
		mGlobalLog = null;
		
		return true;
	}
	
	/**
	 * Gets whether there is a log for the player
	 * @return True if there is
	 */
	public static boolean hasLogFile(OfflinePlayer player)
	{
		return new File(mLogsRoot, sanitiseName(player.getName()) + cFileExt).exists();
	}
	
	/**
	 * Sanitises the player name. Dont think this is really needed, but just to be sure
	 */
	private static String sanitiseName(String name)
	{
		return name.replaceAll("[:/\\\\%\\.]", "");
	}
	
}
