package au.com.mineauz.PlayerSpy.tracdata;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.google.common.io.Files;

import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;

public class LogFileRegistry 
{
	private static File mLogsRoot;
	public static final String cFileExt = ".trackdata";
	public static final String cGlobalFilePrefix = "__";
	
	private static HashMap<String, LogFile> mLoadedLogs = new HashMap<String, LogFile>();
	/**
	 * Sets the directory that the logs are stored in
	 */
	public static void setLogFileDirectory(File directory)
	{
		mLogsRoot = directory;
		mLogsRoot.mkdirs();
	}
	/**
	 * Gets the log file directory
	 */
	public static File getLogFileDirectory()
	{
		return mLogsRoot;
	}
	
	public static LogFile getLogFile(String name)
	{
		if(mLoadedLogs.containsKey(name))
		{
			LogFile log = mLoadedLogs.get(name);
			if(log.isLoaded() || log.isTimingOut())
			{
				log.addReference();
				return log;
			}
		}
		
		File file = new File(mLogsRoot, sanitiseName(name) + cFileExt);
		if(!file.exists())
			return null;
		
		LogFile log = new LogFile();
		if(!log.load(file.getAbsolutePath()))
		{
			LogUtil.severe("Log for " + name + " failed to load. Corrupted?");
			return null;
		}
		
		mLoadedLogs.put(name, log);
		return log;
	}
	
	/**
	 * Gets a loaded log file for the player, or loads one. It does NOT create one if it doesnt exist
	 * Calling this will add a reference onto it so make sure to close it when done
	 * @return null if it didnt exist.
	 */
	public static LogFile getLogFile(OfflinePlayer player)
	{
		return getLogFile(player.getName());
	}
	
	public static LogFile makeReplacementLog(OfflinePlayer player)
	{
		return makeReplacementLog(player.getName(),new File(mLogsRoot, sanitiseName(player.getName()) + cFileExt));
	}
	public static LogFile makeReplacementLog(World world)
	{
		return makeReplacementLog(cGlobalFilePrefix + world.getName(), new File(mLogsRoot, cGlobalFilePrefix + sanitiseName(world.getName()) + cFileExt));
	}
	private static LogFile makeReplacementLog(String logName, File file)
	{
		// We need a log, so make a backup of this one, and make a new one
		int num = 1;
		while(new File(file.getAbsolutePath() + ".bak" + num).exists())	num++;
		
		File backup = new File(file.getAbsolutePath() + ".bak" + num);
		try
		{
			Files.copy(file, backup);
			CrossReferenceIndex.removeLogFile(file);
			
			return LogFile.create(logName, file.getAbsolutePath());
		}
		catch(IOException e)
		{
			e.printStackTrace();
			LogUtil.severe("Cannot generate replacement log file!");
			return null;
		}
	}
	
	public static LogFile getLogFileDelayLoad( OfflinePlayer player )
	{
		if(mLoadedLogs.containsKey(player.getName()))
		{
			LogFile log = mLoadedLogs.get(player.getName());
			if(log.isLoaded() || log.isTimingOut())
			{
				log.addReference();
				return log;
			}
		}
		
		File file = new File(mLogsRoot, sanitiseName(player.getName()) + cFileExt);
		if(!file.exists())
			return null;
		
		LogFile log = new LogFile();
		log.loadAsync(file.getAbsolutePath());
		
		mLoadedLogs.put(player.getName(), log);
		return log;
	}
	/**
	 * Gets a loaded log file for the world, or loads one. It does NOT create one if it doesnt exist
	 * Calling this will add a reference onto it so make sure to close it when done
	 * @return null if it didnt exist.
	 */
	public static LogFile getLogFile(World world)
	{
		return getLogFile(cGlobalFilePrefix + sanitiseName(world.getName()));
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
		
		mLoadedLogs.put(player.getName(), log);
		return log;
	}
	/**
	 * Creates a log file for the world. If it already exists, this will fail
	 * Calling this will add a reference onto it so make sure to close it when done
	 * @return null if it already exists
	 */
	public static LogFile createLogFile(World world)
	{
		File file = new File(mLogsRoot, cGlobalFilePrefix + sanitiseName(world.getName()) + cFileExt);
		if(file.exists())
			return null;
		
		LogFile log = LogFile.create(cGlobalFilePrefix + world.getName(), file.getAbsolutePath());
		
		if(log == null)
			return null;
		
		mLoadedLogs.put(cGlobalFilePrefix + sanitiseName(world.getName()), log);
		return log;
	}
	
	/**
	 * Unloads a log loaded with getLogFile() or createLogFile()
	 * @return True if the log has been unloaded. False if the log wasnt loaded
	 */
	public static boolean unloadLogFile(OfflinePlayer player)
	{
		LogFile log = mLoadedLogs.get(player.getName());
		
		if(log == null)
			return false;
		
		log.close(false);

		if(!log.isLoaded() && !log.isTimingOut())
		{
			return mLoadedLogs.remove(player.getName()) != null;
		}
		
		return true;
	}
	/**
	 * Unloads a log loaded with getLogFile() or createLogFile()
	 * @return True if the log has been unloaded. False if the log wasnt loaded
	 */
	public static boolean unloadLogFile(World world)
	{
		LogFile log = mLoadedLogs.get(cGlobalFilePrefix + sanitiseName(world.getName()));
		
		if(log == null)
			return false;
		
		log.close(false);

		if(!log.isLoaded() && !log.isTimingOut())
		{
			return mLoadedLogs.remove(cGlobalFilePrefix + sanitiseName(world.getName())) != null;
		}
		
		return true;
	}

	/**
	 * Gets whether there is a log for the player
	 * @return True if there is
	 */
	public static boolean hasLogFile(OfflinePlayer player)
	{
		if(player == null)
			return false;
		return new File(mLogsRoot, sanitiseName(player.getName()) + cFileExt).exists();
	}
	/**
	 * Gets whether there is a log for the world
	 * @return True if there is
	 */
	public static boolean hasLogFile(World world)
	{
		if(world == null)
			return false;
		return new File(mLogsRoot, cGlobalFilePrefix + sanitiseName(world.getName()) + cFileExt).exists();
	}
	
	/**
	 * Sanitises the player name. Dont think this is really needed, but just to be sure
	 */
	private static String sanitiseName(String name)
	{
		return name.replaceAll("[:/\\\\%\\.]", "");
	}
	/**
	 * Forcefully unloads all opened log files 
	 */
	public static void unloadAll()
	{
		for(LogFile log : mLoadedLogs.values())
		{
			if(log.isTimingOut())
				log.addReference(); // Cancel the timeout
			
			while(log.isLoaded())
				log.close(true);
		}
		mLoadedLogs.clear();
	}
	
	
}
