package au.com.mineauz.PlayerSpy.monitoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.globalreference.GlobalReferenceFile;
import au.com.mineauz.PlayerSpy.tracdata.SessionEntry;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;
import au.com.mineauz.PlayerSpy.tracdata.LogFileRegistry;

/**
 * The cross reference index provides information on the location of specific data.
 * Some of the features it provides:
 * - links times to sessions in all logfiles
 * - links chunks to sessions in all logfiles
 */
public class CrossReferenceIndex 
{
	private static GlobalReferenceFile instance;
	
	public static GlobalReferenceFile getInstance()
	{
		return instance;
	}
	
	public static void initialize()
	{
		instance = new GlobalReferenceFile();
		File path = new File(SpyPlugin.getInstance().getDataFolder(), "data/reference");
		
		if(!path.exists())
		{
			instance = GlobalReferenceFile.create(path);
			if(instance == null)
				throw new RuntimeException("Failed to start up global reference file.");
		}
		else if(!instance.load(path))
			throw new RuntimeException("Failed to start up global reference file.");
	}
	
	public static boolean removeLogFile(File logFile)
	{
		Validate.notNull(instance, "Reference is not initialized");
		
		try
		{
			instance.beginTransaction();
			instance.removeLog(logFile);
			instance.commitTransaction();
		}
		catch(IOException e)
		{
			instance.rollbackTransaction();
			Debug.logException(e);
			return false;
		}
		catch(IllegalArgumentException e)
		{
			instance.rollbackTransaction();
			Debug.logException(e);
			return false;
		}
		return true;
	}
	
	public static boolean removeLogFile(LogFile log)
	{
		Validate.notNull(instance, "Reference is not initialized");
		
		try
		{
			instance.beginTransaction();
			instance.removeLog(log);
			instance.commitTransaction();
		}
		catch(IOException e)
		{
			instance.rollbackTransaction();
			Debug.logException(e);
			return false;
		}
		return true;
	}
	/**
	 * Adds a session to the database
	 * @param log The logfile the session is in
	 * @param entry The session
	 * @return True if the session was added
	 */
	public static boolean addSession(LogFile log, SessionEntry entry)
	{
		Validate.notNull(instance, "Reference is not initialized");
		
		try
		{
			instance.beginTransaction();
			instance.addSession(entry, log);
			instance.commitTransaction();
		}
		catch(IOException e)
		{
			instance.rollbackTransaction();
			Debug.logException(e);
			return false;
		}
		return true;
	}
	/**
	 * Removes a session from the database
	 * @param log The log the session is in
	 * @param entry The session
	 * @return True if the session as successfully removed
	 */
	public static boolean removeSession(LogFile log, SessionEntry entry)
	{
		Validate.notNull(instance, "Reference is not initialized");
		
		try
		{
			//instance.beginTransaction();
			instance.removeSession(entry, log);
			//instance.commitTransaction();
		}
		catch(IOException e)
		{
			//instance.rollbackTransaction();
			Debug.logException(e);
			return false;
		}
		return true;
	}
	/**
	 * Updates the session information
	 * @param log The log file the session is for
	 * @param entry The session
	 * @return True if the update was successful
	 */
	public static boolean updateSession(LogFile log, SessionEntry entry)
	{
		Validate.notNull(instance, "Reference is not initialized");
		
		try
		{
			instance.beginTransaction();
			instance.updateSession(entry, log);
			instance.commitTransaction();
		}
		catch(IOException e)
		{
			instance.rollbackTransaction();
			Debug.logException(e);
			return false;
		}
		return true;
	}
	/**
	 * Gets all the sessions that contain that chunk
	 * @param location the location to retrieve records for
	 * @return A list of SessionInFile objects that contain the session and the logfile. You should call releaseLastLogs() when you are done with the results
	 */
	public static Results getSessionsFor(Location location)
	{
		Validate.notNull(instance, "Reference is not initialized");
		
		List<au.com.mineauz.PlayerSpy.globalreference.SessionEntry> foundSessions = instance.getSessionsFor(location);
		ArrayList<SessionInFile> results = new ArrayList<CrossReferenceIndex.SessionInFile>();
		
		HashMap<UUID, LogFile> openedLogs = new HashMap<UUID, LogFile>();
		HashSet<String> failedLogs = new HashSet<String>();

		for(au.com.mineauz.PlayerSpy.globalreference.SessionEntry session : foundSessions)
		{
			LogFile log = null;
			if(openedLogs.containsKey(session.fileId))
			{
				log = openedLogs.get(session.fileId);
			}
			else
			{
				// Load it
				String name = instance.getFileName(session.fileId);
				
				if(!failedLogs.contains(name))
				{
					log = LogFileRegistry.getLogFile(name);

					if(log == null)
						failedLogs.add(name);
					else
						openedLogs.put(session.fileId, log);
				}
				
			}
			
			if(log == null)
				continue;
			
			SessionInFile res = new SessionInFile();
			res.Log = log;
			res.Session = log.getSessionById(session.sessionId);
			if(res.Session != null)
				results.add(res);
		}
		
		return new Results(results, openedLogs.values());
	}

	/**
	 * Gets all the sessions that are in the time limits
	 * @param startTime The earliest time to check for
	 * @param endTime The latest date to check for
	 * @return A list of SessionInFile objects that contain the session and the logfile. You should call releaseLastLogs() when you are done with the results
	 */
	public static Results getSessionsFor(long startTime, long endTime)
	{
		Validate.notNull(instance, "Reference is not initialized");
		
		List<au.com.mineauz.PlayerSpy.globalreference.SessionEntry> foundSessions = instance.getSessionsBetween(startTime, endTime);
		ArrayList<SessionInFile> results = new ArrayList<CrossReferenceIndex.SessionInFile>();
		
		HashMap<UUID, LogFile> openedLogs = new HashMap<UUID, LogFile>();
		HashSet<String> failedLogs = new HashSet<String>();

		for(au.com.mineauz.PlayerSpy.globalreference.SessionEntry session : foundSessions)
		{
			LogFile log = null;
			if(openedLogs.containsKey(session.fileId))
			{
				log = openedLogs.get(session.fileId);
			}
			else
			{
				// Load it
				String name = instance.getFileName(session.fileId);
				
				if(!failedLogs.contains(name))
				{
					log = LogFileRegistry.getLogFile(name);

					if(log == null)
						failedLogs.add(name);
					else
						openedLogs.put(session.fileId, log);
				}
				
			}
			
			if(log == null)
				continue;
			
			SessionInFile res = new SessionInFile();
			res.Log = log;
			res.Session = log.getSessionById(session.sessionId);
			if(res.Session != null)
				results.add(res);
		}
		
		return new Results(results, openedLogs.values());
	}
	
	public static Results getSessionsIn(long startTime, long endTime, Location loc, double range, boolean includePlayer)
	{
		Validate.notNull(instance, "Reference is not initialized");
		
		List<au.com.mineauz.PlayerSpy.globalreference.SessionEntry> foundSessions = instance.getSessionsIn(startTime, endTime, loc, range, includePlayer);
		ArrayList<SessionInFile> results = new ArrayList<CrossReferenceIndex.SessionInFile>();
		
		HashMap<UUID, LogFile> openedLogs = new HashMap<UUID, LogFile>();
		HashSet<String> failedLogs = new HashSet<String>();

		for(au.com.mineauz.PlayerSpy.globalreference.SessionEntry session : foundSessions)
		{
			LogFile log = null;
			if(openedLogs.containsKey(session.fileId))
			{
				log = openedLogs.get(session.fileId);
			}
			else
			{
				// Load it
				String name = instance.getFileName(session.fileId);
				
				if(!failedLogs.contains(name))
				{
					log = LogFileRegistry.getLogFile(name);

					if(log == null)
						failedLogs.add(name);
					else
						openedLogs.put(session.fileId, log);
				}
				
			}
			
			if(log == null)
				continue;
			
			SessionInFile res = new SessionInFile();
			res.Log = log;
			res.Session = log.getSessionById(session.sessionId);
			if(res.Session != null)
				results.add(res);
		}
		
		return new Results(results, openedLogs.values());
	}
	public static class SessionInFile
	{
		public SessionEntry Session;
		public LogFile Log;
	}
	
	public static class Results
	{
		public Results(List<SessionInFile> sessions, Collection<LogFile> openedLogs)
		{
			foundSessions = sessions;
			mOpenedLogs = openedLogs;
		}
		
		public List<SessionInFile> foundSessions;
		
		private Collection<LogFile> mOpenedLogs;
		public void release()
		{
			for(LogFile log : mOpenedLogs)
			{
				if(log.getName().startsWith(LogFileRegistry.cGlobalFilePrefix))
					LogFileRegistry.unloadLogFile(Bukkit.getWorld(log.getName().substring(LogFileRegistry.cGlobalFilePrefix.length())));
				else
					LogFileRegistry.unloadLogFile(Bukkit.getOfflinePlayer(log.getName()));
			}
			
			mOpenedLogs.clear();
		}
		
		public int getLogCount()
		{
			return mOpenedLogs.size();
		}
		
	}
}
