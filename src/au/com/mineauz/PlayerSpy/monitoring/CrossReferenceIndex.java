package au.com.mineauz.PlayerSpy.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.io.File;
import java.io.IOException;
import java.sql.*;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.IndexEntry;
import au.com.mineauz.PlayerSpy.InterlinkFile;
import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.Pair;
import au.com.mineauz.PlayerSpy.SpyPlugin;

/**
 * The cross reference index provides information on the location of specific data.
 * Some of the features it provides:
 * - links times to sessions in all logfiles
 * - links chunks to sessions in all logfiles
 */
public class CrossReferenceIndex 
{
	public static final CrossReferenceIndex instance;
	
	static
	{
		instance = new CrossReferenceIndex();
	}

	private InterlinkFile mFile;
	private CrossReferenceIndex()
	{
	}
	
	public void close()
	{
		mFile.close();
	}
	
	public boolean initialize()
	{
		File location = new File(SpyPlugin.getInstance().getDataFolder(), "link");
		if(location.exists())
		{
			mFile = new InterlinkFile();
			if(!mFile.load(location))
				return false;
		}
		else
		{
			mFile = InterlinkFile.create(location);
		}
		
		if(mFile == null)
			return false;
		
		return true;
	}
	
	public synchronized boolean removeLogFile(LogFile log)
	{
		return false;
	}
	/**
	 * Adds a session to the database
	 * @param log The logfile the session is in
	 * @param entry The session
	 * @param chunks The chunks in the session
	 * @return True if the session was added
	 */
	public synchronized boolean addSession(LogFile log, IndexEntry entry, List<Chunk> chunks)
	{
		return mFile.addSession(log, entry, null, chunks);
	}
	/**
	 * Adds a session to the database
	 * @param log The logfile the session is in
	 * @param entry The session
	 * @param owner The owner of the session
	 * @param chunks The chunks in the session
	 * @return True if the session was added
	 */
	public synchronized boolean addSession(LogFile log, IndexEntry entry, String owner, List<Chunk> chunks)
	{
		return mFile.addSession(log, entry, owner, chunks);
	}
	
	/**
	 * Removes a session from the database
	 * @param log The log the session is in
	 * @param entry The session
	 * @return True if the session as successfully removed
	 */
	public synchronized boolean removeSession(LogFile log, IndexEntry entry)
	{
		return mFile.removeSession(log, entry);
	}
	/**
	 * Updates the session information
	 * @param log The log file the session is for
	 * @param entry The session
	 * @return True if the update was successful
	 */
	public synchronized boolean updateSession(LogFile log, IndexEntry entry, List<Chunk> chunks)
	{
		return mFile.updateSession(log, entry, chunks);
	}
	/**
	 * Updates the session index
	 * @param log The logfile the session is in
	 * @param oldIndex The index it was before the move
	 * @param newIndex The index it is now
	 * @return True if the update was successful
	 */
	public synchronized boolean moveSession(LogFile log, int oldIndex, int newIndex)
	{
		return mFile.moveSession(log, oldIndex, newIndex);
	}
	/**
	 * Gets all the sessions that contain that chunk
	 * @param location the location to check for
	 * @return A list of SessionInFile objects that contain the session and the logfile. You should call releaseLastLogs() when you are done with the results
	 */
	public synchronized List<SessionInFile> getSessionsFor(Location location)
	{
		//List<Pair<String, Integer>> results = mFile.getSessionsMatching(new InterlinkFile.Predicate().containsLocation(location));
		// TODO: getSessionsFor
		return null;
	}
	/**
	 * Gets all the session that contain that chunk and are within the time limit
	 * @param location the location to check for
	 * @param startTime The earilist date you wish to check for
	 * @param endTime The latest date you with to check for
	 * @return A list of SessionInFile objects that contain the session and the logfile. You should call releaseLastLogs() when you are done with the results 
	 */
	public synchronized List<SessionInFile> getSessionsFor(Location location, long startTime, long endTime)
	{
		//List<Pair<String, Integer>> results = mFile.getSessionsMatching(new InterlinkFile.Predicate().containsLocation(location).startsAt(startTime).endsAt(endTime));
		// TODO: getSessionsFor
		return null;
		
	}
	/**
	 * Gets all the sessions that are in the time limits
	 * @param startTime The earliest time to check for
	 * @param endTime The latest date to check for
	 * @return A list of SessionInFile objects that contain the session and the logfile. You should call releaseLastLogs() when you are done with the results
	 */
	public synchronized List<SessionInFile> getSessionsFor(long startTime, long endTime)
	{
		//List<Pair<String, Integer>> results = mFile.getSessionsMatching(new InterlinkFile.Predicate().startsAt(startTime).endsAt(endTime));
		// TODO: getSessionsFor
		return null;
	}
	
	/**
	 * Closes or decreases reference counts of opened logs from the getSessionsFor() methods
	 */
	public synchronized void releaseLastLogs()
	{
		for(Entry<Integer, LogFile> ent : mOpenedLogs.entrySet())
		{
			ent.getValue().close();
			if(!ent.getValue().isLoaded())
			{
				if(ent.getValue().getName().equals("__global"))
					LogFileRegistry.unloadGlobalLogFile();
				else
					LogFileRegistry.unloadLogFile(Bukkit.getOfflinePlayer(ent.getValue().getName()));
			}
		}
		
		mOpenedLogs.clear();
	}
	
	private HashMap<Integer, LogFile> mOpenedLogs = new HashMap<Integer, LogFile>();
	public static class SessionInFile
	{
		public IndexEntry Session;
		public LogFile Log;
	}
}
