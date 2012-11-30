package au.com.mineauz.PlayerSpy.monitoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.sql.*;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.IndexEntry;
import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.Utilities.SafeChunk;

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

	private Connection mDatabaseConnection = null;
	private PreparedStatement mAddFileStatement;
	private PreparedStatement mAddSessionStatement;
	private PreparedStatement mAddChunkStatement;
	private PreparedStatement mUpdateSessionStatement;
		private PreparedStatement mDeleteFileStatement;
	private PreparedStatement mDeleteSessionStatement;
	//private PreparedStatement mDeleteChunkStatement;
	private PreparedStatement mSelectFileStatement;
	private PreparedStatement mSelectFileByIDStatement;
	//private PreparedStatement mSelectSessionByIDStatement;
		private PreparedStatement mSelectSessionByChunkStatement;
	private PreparedStatement mSelectSessionByChunkBetweenTimeStatement;
	private PreparedStatement mSelectSessionBetweenTimeStatement;
	//private PreparedStatement mSelectSessionEarlierThanTimeStatement;
	//private PreparedStatement mSelectSessionLaterThanTimeStatement;
	//private PreparedStatement mSelectSessionContainsTimeStatement;
	private PreparedStatement mSelectChunksBySessionStatement;
	
	private HashMap<LogFile, Integer> mKnownFileIDs;
	private CrossReferenceIndex()
	{
		mKnownFileIDs = new HashMap<LogFile, Integer>();
	}
	
	private void tryRollback()
	{
		try
		{
			mDatabaseConnection.rollback();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public void close()
	{
		try {
			mAddFileStatement.close();
			mAddSessionStatement.close();
			mAddChunkStatement.close();
			mUpdateSessionStatement.close();
			mDeleteFileStatement.close();
			mDeleteSessionStatement.close();
			
			mSelectFileStatement.close();
			mSelectFileByIDStatement.close();
			
			mSelectSessionByChunkStatement.close();
			mSelectSessionByChunkBetweenTimeStatement.close();
			
			mSelectSessionBetweenTimeStatement.close();
			mSelectChunksBySessionStatement.close();
			
			mDatabaseConnection.commit();
			mDatabaseConnection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public boolean initialize()
	{
		try
		{
			Class.forName("org.sqlite.JDBC");
			mDatabaseConnection = DriverManager.getConnection("jdbc:sqlite:" + LogFileRegistry.getLogFileDirectory().getPath() + "/xlink.db");
			mDatabaseConnection.setAutoCommit(false);
			
			// Begin building the sql statements to use
			Statement create = mDatabaseConnection.createStatement();
			create.executeUpdate("CREATE TABLE IF NOT EXISTS FileMap (FILE_ID INTEGER PRIMARY KEY ASC AUTOINCREMENT, PLAYER VARCHAR)");
			create.executeUpdate("CREATE TABLE IF NOT EXISTS SessionMap (SESSION_ID INTEGER PRIMARY KEY, FILE_ID INTEGER REFERENCES FileMap(FILE_ID) ON DELETE CASCADE, START_DATE INTEGER, END_DATE INTEGER)");
			create.executeUpdate("CREATE TABLE IF NOT EXISTS Chunks (X INTEGER, Z INTEGER, WORLD INTEGER, SESSION_ID INTEGER REFERENCES SessionMap(SESSION_ID) ON DELETE CASCADE)");

			create.close();
			mDatabaseConnection.commit();
			
			
			mAddFileStatement = mDatabaseConnection.prepareStatement("INSERT INTO FileMap (PLAYER) VALUES (?);");
			mAddSessionStatement = mDatabaseConnection.prepareStatement("INSERT INTO SessionMap (SESSION_ID, FILE_ID, START_DATE, END_DATE) VALUES (?,?,?,?);");
			mAddChunkStatement = mDatabaseConnection.prepareStatement("INSERT INTO Chunks VALUES (?,?,?,?);");
			
			mUpdateSessionStatement = mDatabaseConnection.prepareStatement("UPDATE SessionMap SET START_DATE = ?, END_DATE = ? WHERE SESSION_ID = ?;");
			mDeleteFileStatement = mDatabaseConnection.prepareStatement("DELETE FROM FileMap WHERE FILE_ID = ?;");
			mDeleteSessionStatement = mDatabaseConnection.prepareStatement("DELETE FROM SessionMap WHERE SESSION_ID = ?;");
			//mDeleteChunkStatement = mDatabaseConnection.prepareStatement("DELETE FROM Chunks WHERE X = ? AND Z = ? AND WORLD = ? AND SESSION_ID = ?;");
			
			mSelectFileStatement = mDatabaseConnection.prepareStatement("SELECT * FROM FileMap WHERE PLAYER = ?;");
			mSelectFileByIDStatement = mDatabaseConnection.prepareStatement("SELECT * FROM FileMap WHERE FILE_ID = ?;");
			//mSelectSessionByIDStatement = mDatabaseConnection.prepareStatement("SELECT * FROM SessionMap WHERE SESSION_ID = ?;");
			mSelectSessionByChunkStatement = mDatabaseConnection.prepareStatement("SELECT SessionMap.SESSION_ID, SessionMap.FILE_ID, SessionMap.START_DATE, SessionMap.END_DATE FROM Chunks JOIN SessionMap ON Chunks.SESSION_ID = SessionMap.SESSION_ID WHERE Chunks.X = ? AND Chunks.Z = ? AND Chunks.WORLD = ?;");
			mSelectSessionByChunkBetweenTimeStatement = mDatabaseConnection.prepareStatement("SELECT SessionMap.SESSION_ID, SessionMap.FILE_ID, SessionMap.START_DATE, SessionMap.END_DATE FROM Chunks JOIN SessionMap ON Chunks.SESSION_ID = SessionMap.SESSION_ID WHERE Chunks.X = ? AND Chunks.Z = ? AND Chunks.WORLD = ? AND ((SessionMap.START_DATE >= ? AND SessionMap.START_DATE <= ?) OR (SessionMap.END_DATE >= ? AND SessionMap.END_DATE <= ?) OR (SessionMap.START_DATE < ? AND SessionMap.END_DATE > ?) OR (SessionMap.START_DATE < ? AND SessionMap.END_DATE > ?));");
			mSelectSessionBetweenTimeStatement = mDatabaseConnection.prepareStatement("SELECT  SESSION_ID, FILE_ID, START_DATE, END_DATE FROM SessionMap WHERE (START_DATE >= ? AND START_DATE <= ?) OR (END_DATE >= ? AND END_DATE <= ?) OR (START_DATE < ? AND END_DATE > ?) OR (START_DATE < ? AND END_DATE > ?);");
			//mSelectSessionLaterThanTimeStatement = mDatabaseConnection.prepareStatement("SELECT * FROM SessionMap WHERE END_DATE > ? ORDER BY END_DATE DESC;");
			//mSelectSessionContainsTimeStatement = mDatabaseConnection.prepareStatement("SELECT * FROM SessionMap WHERE ? BETWEEN START_DATE AND END_DATE ORDER BY END_DATE DESC;");
			mSelectChunksBySessionStatement = mDatabaseConnection.prepareStatement("SELECT * FROM Chunks WHERE SESSION_ID = ?");
			
			return true;
		}
		catch(ClassNotFoundException e)
		{
			e.printStackTrace();
			return false;
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Registers a new log file in the database
	 * @param log
	 * @return
	 */
	public synchronized boolean addLogFile(LogFile log)
	{
		try
		{
			mAddFileStatement.setString(1, log.getName());
			mAddFileStatement.executeUpdate();
			mDatabaseConnection.commit();
			return true;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			tryRollback();
			return false;
		}
	}
	private synchronized int getFileId(LogFile log)
	{
		if(mKnownFileIDs.containsKey(log))
			return mKnownFileIDs.get(log);
		
		try
		{
			mSelectFileStatement.setString(1, log.getName());
			ResultSet rs = mSelectFileStatement.executeQuery();
			if(!rs.next())
			{
				rs.close();
				return -1;
			}
			int fileId = rs.getInt(1);
			mKnownFileIDs.put(log, fileId);
			rs.close();
			return fileId;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			return -1;
		}
	}
	private synchronized int getFileId(String logName)
	{
		for(Entry<LogFile, Integer> entry : mKnownFileIDs.entrySet())
		{
			if (entry.getKey().getName().equals(logName))
				return entry.getValue();
		}
		
		try
		{
			mSelectFileStatement.setString(1, logName);
			ResultSet rs = mSelectFileStatement.executeQuery();
			if(!rs.next())
			{
				rs.close();
				return -1;
			}
			int fileId = rs.getInt(1);

			rs.close();
			return fileId;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			return -1;
		}
	}
	public synchronized boolean removeLogFile(String logName)
	{
		try
		{
			// Get the file id
			int fileId = getFileId(logName);
			if(fileId == -1)
				return false;
			
			// Remove the file
			mDeleteFileStatement.setInt(1, fileId);
			mDeleteFileStatement.execute();
			mDatabaseConnection.commit();
			
			for(Entry<LogFile, Integer> entry : mKnownFileIDs.entrySet())
			{
				if (entry.getKey().getName().equals(logName))
				{
					mKnownFileIDs.remove(entry.getKey());
					break;
				}
			}
			
			return true;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			tryRollback();
		}
		
		return false;
	}
	public synchronized boolean removeLogFile(LogFile log)
	{
		try
		{
			// Get the file id
			int fileId = getFileId(log);
			if(fileId == -1)
				return false;
			
			// Remove the file
			mDeleteFileStatement.setInt(1, fileId);
			mDeleteFileStatement.execute();
			mDatabaseConnection.commit();
			
			mKnownFileIDs.remove(log);
			
			return true;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			tryRollback();
		}
		
		return false;
	}
	/**
	 * Adds a session to the database
	 * @param log The logfile the session is in
	 * @param entry The session
	 * @param chunks The chunks in the session
	 * @return True if the session was added
	 */
	public synchronized boolean addSession(LogFile log, IndexEntry entry, List<SafeChunk> chunks)
	{
		try
		{
			// Get the file id
			int fileId = getFileId(log);
			if(fileId == -1)
				return false;
			
			LogUtil.finer("Adding session to reference");
			mAddSessionStatement.setInt(1, entry.Id);
			mAddSessionStatement.setInt(2, fileId);
			mAddSessionStatement.setLong(3, entry.StartTimestamp);
			mAddSessionStatement.setLong(4, entry.EndTimestamp);
			
			mAddSessionStatement.executeUpdate();
			
			LogUtil.finer("Session ID = " + entry.Id);
			
			mAddChunkStatement.clearBatch();
			int count = 0;
			for(SafeChunk chunk : chunks)
			{
				if(chunk == null)
					continue;
				count++;
				mAddChunkStatement.setInt(1, chunk.X);
				mAddChunkStatement.setInt(2, chunk.Z);
				mAddChunkStatement.setInt(3, chunk.WorldHash);
				mAddChunkStatement.setInt(4, entry.Id);
				mAddChunkStatement.addBatch();
			}
			LogUtil.finer("Adding " + count + " Chunks");
			mAddChunkStatement.executeBatch();
			
			mDatabaseConnection.commit();
			return true;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			tryRollback();
		}
		return false;
	}
	/**
	 * Removes a session from the database
	 * @param log The log the session is in
	 * @param entry The session
	 * @return True if the session as successfully removed
	 */
	public synchronized boolean removeSession(LogFile log, IndexEntry entry)
	{
		try
		{
			int fileId = getFileId(log);
			if(fileId == -1)
				return false;
			
			mDeleteSessionStatement.setInt(1, entry.Id);
			mDeleteSessionStatement.execute();
			
			mDatabaseConnection.commit();
			return true;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			tryRollback();
			return false;
		}
	}
	/**
	 * Updates the session information
	 * @param log The log file the session is for
	 * @param entry The session
	 * @return True if the update was successful
	 */
	public synchronized boolean updateSession(LogFile log, IndexEntry entry, List<SafeChunk> chunks)
	{
		try
		{
			LogUtil.finer("Session Id = " + entry.Id);
			LogUtil.finer("Updating Basic Session Info");
			mUpdateSessionStatement.setLong(1, entry.StartTimestamp);
			mUpdateSessionStatement.setLong(2, entry.EndTimestamp);
			mUpdateSessionStatement.setInt(3, entry.Id);
			
			mUpdateSessionStatement.executeUpdate();
			
			// Find what chunks are new
			LogUtil.finer("Finding New Chunks");
			mSelectChunksBySessionStatement.setInt(1, entry.Id);
			ResultSet existingChunks = mSelectChunksBySessionStatement.executeQuery();

			while(existingChunks.next())
			{
				// Columns are: X, Z, WORLD, SESSION_ID
				for(SafeChunk chunk : chunks)
				{
					if(chunk.X == existingChunks.getInt(1) && chunk.Z == existingChunks.getInt(2) && chunk.WorldHash == existingChunks.getInt(3))
					{
						chunks.remove(chunk);
						break;
					}
				}
				
				if(chunks.isEmpty())
					break;
			}
			existingChunks.close();
			
			// Add the chunks
			mAddChunkStatement.clearBatch();
			int count = 0;
			for(SafeChunk chunk : chunks)
			{
				count++;
				mAddChunkStatement.setInt(1, chunk.X);
				mAddChunkStatement.setInt(2, chunk.Z);
				mAddChunkStatement.setInt(3, chunk.WorldHash);
				mAddChunkStatement.setInt(4, entry.Id);
				mAddChunkStatement.addBatch();
			}
			LogUtil.finer("Adding " + count + " new chunks");
			mAddChunkStatement.executeBatch();
			
			
			mDatabaseConnection.commit();
			return true;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			tryRollback();

			return false;
		}
	}
	/**
	 * Gets all the sessions that contain that chunk
	 * @param chunk The chunk to check for
	 * @return A list of SessionInFile objects that contain the session and the logfile. You should call releaseLastLogs() when you are done with the results
	 */
	public synchronized Results getSessionsFor(SafeChunk chunk)
	{
		try
		{
			mSelectSessionByChunkStatement.setInt(1, chunk.X);
			mSelectSessionByChunkStatement.setInt(2, chunk.Z);
			mSelectSessionByChunkStatement.setInt(3, chunk.WorldHash);
			
			ResultSet rs = mSelectSessionByChunkStatement.executeQuery();
			ArrayList<SessionInFile> results = new ArrayList<CrossReferenceIndex.SessionInFile>();
			
			HashMap<Integer, LogFile> openedLogs = new HashMap<Integer, LogFile>();
			HashSet<String> failedLogs = new HashSet<String>();
			
			while(rs.next())
			{
				// Columns are: SESSION_ID, FILE_ID, SESSION_INDEX, START_DATE, END_DATE
				int fileId = rs.getInt(2);
				LogFile log = null;
				if(openedLogs.containsKey(fileId))
				{
					log = openedLogs.get(fileId);
				}
				else
				{
					// Load it
					mSelectFileByIDStatement.setInt(1, fileId);
					ResultSet fileRs = mSelectFileByIDStatement.executeQuery();
					
					if(!fileRs.next())
					{
						fileRs.close();
						continue;
					}
					
					String name = fileRs.getString(2);
					
					if(!failedLogs.contains(name))
					{
						if(name.startsWith(LogFileRegistry.cGlobalFilePrefix))
						{
							World world = Bukkit.getWorld(name.substring(LogFileRegistry.cGlobalFilePrefix.length()));
							log = LogFileRegistry.getLogFile(world);
						}
						else
						{
							OfflinePlayer player = Bukkit.getOfflinePlayer(name);
							log = LogFileRegistry.getLogFile(player);
						}
						
						if(log == null)
							failedLogs.add(name);
					}
					fileRs.close();
					
					if(log == null)
						continue;
					
					openedLogs.put(fileId, log);
				}
				
				SessionInFile res = new SessionInFile();
				res.Log = log;
				res.Session = log.getSessionById(rs.getInt(1));
				if(res.Session != null)
					results.add(res);
			}
			
			rs.close();
			
			return new Results(results, openedLogs.values());
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			return new Results(new ArrayList<CrossReferenceIndex.SessionInFile>(), new ArrayList<LogFile>());
		}
	}
	/**
	 * Gets all the session that contain that chunk and are within the time limit
	 * @param chunk The chunk to check for
	 * @param startTime The earilist date you wish to check for
	 * @param endTime The latest date you with to check for
	 * @return A list of SessionInFile objects that contain the session and the logfile. You should call releaseLastLogs() when you are done with the results 
	 */
	public synchronized Results getSessionsFor(SafeChunk chunk, long startTime, long endTime)
	{
		try
		{
			mSelectSessionByChunkBetweenTimeStatement.setInt(1, chunk.X);
			mSelectSessionByChunkBetweenTimeStatement.setInt(2, chunk.Z);
			mSelectSessionByChunkBetweenTimeStatement.setInt(3, chunk.WorldHash);
			mSelectSessionByChunkBetweenTimeStatement.setLong(4, startTime);
			mSelectSessionByChunkBetweenTimeStatement.setLong(5, endTime);
			mSelectSessionByChunkBetweenTimeStatement.setLong(6, startTime);
			mSelectSessionByChunkBetweenTimeStatement.setLong(7, endTime);
			mSelectSessionByChunkBetweenTimeStatement.setLong(8, startTime);
			mSelectSessionByChunkBetweenTimeStatement.setLong(9, startTime);
			mSelectSessionByChunkBetweenTimeStatement.setLong(10, endTime);
			mSelectSessionByChunkBetweenTimeStatement.setLong(11, endTime);
			
			ResultSet rs = mSelectSessionByChunkBetweenTimeStatement.executeQuery();
			ArrayList<SessionInFile> results = new ArrayList<CrossReferenceIndex.SessionInFile>();
			HashMap<Integer, LogFile> openedLogs = new HashMap<Integer, LogFile>();
			HashSet<String> failedLogs = new HashSet<String>();
			
			while(rs.next())
			{
				// Columns are: SESSION_ID, FILE_ID, SESSION_INDEX, START_DATE, END_DATE
				int fileId = rs.getInt(2);
				LogFile log = null;
				if(openedLogs.containsKey(fileId))
				{
					log = openedLogs.get(fileId);
				}
				else
				{
					// Load it
					mSelectFileByIDStatement.setInt(1, fileId);
					ResultSet fileRs = mSelectFileByIDStatement.executeQuery();
					
					if(!fileRs.next())
					{
						fileRs.close();
						continue;
					}
					
					String name = fileRs.getString(2);
					if(!failedLogs.contains(name))
					{
						if(name.startsWith(LogFileRegistry.cGlobalFilePrefix))
						{
							World world = Bukkit.getWorld(name.substring(LogFileRegistry.cGlobalFilePrefix.length()));
							log = LogFileRegistry.getLogFile(world);
						}
						else
						{
							OfflinePlayer player = Bukkit.getOfflinePlayer(name);
							log = LogFileRegistry.getLogFile(player);
						}
						if(log == null)
							failedLogs.add(name);
					}
					fileRs.close();
					
					if(log == null)
						continue;
					
					openedLogs.put(fileId, log);
				}
				
				SessionInFile res = new SessionInFile();
				res.Log = log;
				res.Session = log.getSessionById(rs.getInt(1));
				if(res.Session != null)
					results.add(res);
			}
			
			rs.close();
			
			return new Results(results, openedLogs.values());
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			return new Results(new ArrayList<CrossReferenceIndex.SessionInFile>(), new ArrayList<LogFile>());
		}
	}
	/**
	 * Gets all the sessions that are in the time limits
	 * @param startTime The earliest time to check for
	 * @param endTime The latest date to check for
	 * @return A list of SessionInFile objects that contain the session and the logfile. You should call releaseLastLogs() when you are done with the results
	 */
	public synchronized Results getSessionsFor(long startTime, long endTime)
	{
		try
		{
			mSelectSessionBetweenTimeStatement.setLong(1, startTime);
			mSelectSessionBetweenTimeStatement.setLong(2, endTime);
			mSelectSessionBetweenTimeStatement.setLong(3, startTime);
			mSelectSessionBetweenTimeStatement.setLong(4, endTime);
			mSelectSessionBetweenTimeStatement.setLong(5, startTime);
			mSelectSessionBetweenTimeStatement.setLong(6, startTime);
			mSelectSessionBetweenTimeStatement.setLong(7, endTime);
			mSelectSessionBetweenTimeStatement.setLong(8, endTime);
			
			ResultSet rs = mSelectSessionBetweenTimeStatement.executeQuery();
			ArrayList<SessionInFile> results = new ArrayList<CrossReferenceIndex.SessionInFile>();
			HashMap<Integer, LogFile> openedLogs = new HashMap<Integer, LogFile>();
			HashSet<String> failedLogs = new HashSet<String>();
			
			while(rs.next())
			{
				// Columns are: SESSION_ID, FILE_ID, SESSION_INDEX, START_DATE, END_DATE
				int fileId = rs.getInt(2);
				LogFile log = null;
				if(openedLogs.containsKey(fileId))
				{
					log = openedLogs.get(fileId);
				}
				else
				{
					// Load it
					mSelectFileByIDStatement.setInt(1, fileId);
					ResultSet fileRs = mSelectFileByIDStatement.executeQuery();
					
					if(!fileRs.next())
					{
						fileRs.close();
						continue;
					}
					
					String name = fileRs.getString(2);
					if(!failedLogs.contains(name))
					{
						if(name.startsWith(LogFileRegistry.cGlobalFilePrefix))
						{
							World world = Bukkit.getWorld(name.substring(LogFileRegistry.cGlobalFilePrefix.length()));
							log = LogFileRegistry.getLogFile(world);
						}
						else
						{
							OfflinePlayer player = Bukkit.getOfflinePlayer(name);
							log = LogFileRegistry.getLogFile(player);
						}
						if(log == null)
							failedLogs.add(name);
					}
					fileRs.close();
					
					if(log == null)
						continue;
					
					openedLogs.put(fileId, log);
				}
				
				SessionInFile res = new SessionInFile();
				res.Log = log;
				res.Session = log.getSessionById(rs.getInt(1));
				if(res.Session != null)
					results.add(res);
			}
			
			rs.close();
			
			return new Results(results, openedLogs.values());
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			return new Results(new ArrayList<CrossReferenceIndex.SessionInFile>(), new ArrayList<LogFile>());
		}
	}
	
	public static class SessionInFile
	{
		public IndexEntry Session;
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
