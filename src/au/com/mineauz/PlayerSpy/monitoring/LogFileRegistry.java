package au.com.mineauz.PlayerSpy.monitoring;

import java.io.File;
import java.util.HashMap;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;

public class LogFileRegistry 
{
	private static File mLogsRoot;
	public static final String cFileExt = ".trackdata";
	public static final String cGlobalFilePrefix = "__";
	
	private static HashMap<String, LogFile> mLoadedLogs = new HashMap<String, LogFile>();
	
	private static HashMap<World, LogFile> mLoadedGlobalLogs = new HashMap<World, LogFile>();
	
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
		if(!log.load(file.getAbsolutePath()))
		{
			LogUtil.warning("Player log for " + player.getName() + " failed to load. Corrupted?");
			return null;
		}
		
		mLoadedLogs.put(player.getName(), log);
		return log;
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
		if(mLoadedGlobalLogs.containsKey(world))
		{
			LogFile log = mLoadedGlobalLogs.get(world);
			if(log.isLoaded())
			{
				log.addReference();
				return log;
			}
		}
		
		File file = new File(mLogsRoot, cGlobalFilePrefix + sanitiseName(world.getName()) + cFileExt);
		if(!file.exists())
			return null;
		
		LogFile log = new LogFile();
		if(!log.load(file.getAbsolutePath()))
		{
			LogUtil.warning("World log for " + world.getName() + " failed to load. Corrupted?");
			return null;
		}
		
		mLoadedGlobalLogs.put(world, log);
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
		
		LogFile log = LogFile.createGlobal(cGlobalFilePrefix + world.getName(), file.getAbsolutePath());
		
		if(log == null)
			return null;
		
		mLoadedGlobalLogs.put(world, log);
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
		LogFile log = mLoadedGlobalLogs.get(world);
		
		if(log == null)
			return false;
		
		log.close(false);

		if(!log.isLoaded() && !log.isTimingOut())
		{
			return mLoadedGlobalLogs.remove(world) != null;
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
		
		for(LogFile log : mLoadedGlobalLogs.values())
		{
			if(log.isTimingOut())
				log.addReference(); // Cancel the timeout
			while(log.isLoaded())
				log.close(true);
		}
		mLoadedGlobalLogs.clear();
	}
	
	
}
