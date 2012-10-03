package au.com.mineauz.PlayerSpy.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.sql.*;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.IndexEntry;
import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;

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
	private PreparedStatement mUpdateSessionIndexStatement;
	private PreparedStatement mDeleteFileStatement;
	private PreparedStatement mDeleteSessionStatement;
	//private PreparedStatement mDeleteChunkStatement;
	private PreparedStatement mSelectFileStatement;
	private PreparedStatement mSelectFileByIDStatement;
	//private PreparedStatement mSelectSessionByIDStatement;
	private PreparedStatement mSelectSessionStatement;
	private PreparedStatement mSelectSessionByChunkStatement;
	//private PreparedStatement mSelectSessionEarlierThanTimeStatement;
	//private PreparedStatement mSelectSessionLaterThanTimeStatement;
	//private PreparedStatement mSelectSessionContainsTimeStatement;
	private PreparedStatement mSelectChunksBySessionStatement;
	
	private HashMap<LogFile, Integer> mKnownFileIDs;
	private CrossReferenceIndex()
	{
		mKnownFileIDs = new HashMap<LogFile, Integer>();
	}
	
	public void close()
	{
		try {
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
			create.executeUpdate("CREATE TABLE IF NOT EXISTS SessionMap (SESSION_ID INTEGER PRIMARY KEY ASC AUTOINCREMENT, FILE_ID INTEGER REFERENCES FileMap(FILE_ID) ON DELETE CASCADE, SESSION_INDEX INTEGER, START_DATE INTEGER, END_DATE INTEGER)");
			create.executeUpdate("CREATE TABLE IF NOT EXISTS Chunks (X INTEGER, Z INTEGER, WORLD INTEGER, SESSION_ID INTEGER REFERENCES SessionMap(SESSION_ID) ON DELETE CASCADE)");

			create.close();
			mDatabaseConnection.commit();
			
			
			mAddFileStatement = mDatabaseConnection.prepareStatement("INSERT INTO FileMap (PLAYER) VALUES (?);");
			mAddSessionStatement = mDatabaseConnection.prepareStatement("INSERT INTO SessionMap (FILE_ID, SESSION_INDEX, START_DATE, END_DATE) VALUES (?,?,?,?);");
			mAddChunkStatement = mDatabaseConnection.prepareStatement("INSERT INTO Chunks VALUES (?,?,?,?);");
			
			mUpdateSessionStatement = mDatabaseConnection.prepareStatement("UPDATE SessionMap SET SESSION_INDEX = ?, START_DATE = ?, END_DATE = ? WHERE SESSION_ID = ?;");
			mUpdateSessionIndexStatement = mDatabaseConnection.prepareStatement("UPDATE SessionMap SET SESSION_INDEX = ? WHERE SESSION_ID = ?");
			mDeleteFileStatement = mDatabaseConnection.prepareStatement("DELETE FROM FileMap WHERE FILE_ID = ?;");
			mDeleteSessionStatement = mDatabaseConnection.prepareStatement("DELETE FROM SessionMap WHERE SESSION_ID = ?;");
			//mDeleteChunkStatement = mDatabaseConnection.prepareStatement("DELETE FROM Chunks WHERE X = ? AND Z = ? AND WORLD = ? AND SESSION_ID = ?;");
			
			mSelectFileStatement = mDatabaseConnection.prepareStatement("SELECT * FROM FileMap WHERE PLAYER = ?;");
			mSelectFileByIDStatement = mDatabaseConnection.prepareStatement("SELECT * FROM FileMap WHERE FILE_ID = ?;");
			//mSelectSessionByIDStatement = mDatabaseConnection.prepareStatement("SELECT * FROM SessionMap WHERE SESSION_ID = ?;");
			mSelectSessionStatement = mDatabaseConnection.prepareStatement("SELECT * FROM SessionMap WHERE FILE_ID = ? AND SESSION_INDEX = ?;");
			mSelectSessionByChunkStatement = mDatabaseConnection.prepareStatement("SELECT SessionMap.SESSION_ID, SessionMap.FILE_ID, SessionMap.SESSION_INDEX, SessionMap.START_DATE, SessionMap.END_DATE FROM Chunks JOIN SessionMap ON Chunks.SESSION_ID = SessionMap.SESSION_ID WHERE Chunks.X = ? AND Chunks.Z = ? AND Chunks.WORLD = ?;");
			//mSelectSessionEarlierThanTimeStatement = mDatabaseConnection.prepareStatement("SELECT * FROM SessionMap WHERE START_DATE < ? ORDER BY END_DATE DESC;");
			//mSelectSessionLaterThanTimeStatement = mDatabaseConnection.prepareStatement("SELECT * FROM SessionMap WHERE END_DATE > ? ORDER BY END_DATE DESC;");
			//mSelectSessionContainsTimeStatement = mDatabaseConnection.prepareStatement("SELECT * FROM SessionMap WHERE ? BETWEEN START_DATE AND END_DATE ORDER BY END_DATE DESC;");
			mSelectChunksBySessionStatement = mDatabaseConnection.prepareStatement("SELECT * FROM Chunks WHERE SESSION_ID = ?");
			
			return true;
		}
		catch(ClassNotFoundException e)
		{
			LogUtil.severe(e.getMessage());
			return false;
		} 
		catch (SQLException e) 
		{
			LogUtil.severe(e.getMessage());
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
				return -1;
			int fileId = rs.getInt(1);
			mKnownFileIDs.put(log, fileId);
			rs.close();
			return fileId;
		}
		catch(SQLException e)
		{
			LogUtil.severe(e.getMessage());
			return -1;
		}
	}
	/**
	 * Gets the world as a hash. Used for storing which world something is in rather than the using the name string
	 * @param world
	 * @return
	 */
	private synchronized int getWorldHash(World world)
	{
		return world.getName().hashCode();
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
			
			return true;
		}
		catch(SQLException e)
		{
			LogUtil.severe(e.getMessage());
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
	public synchronized boolean addSession(LogFile log, IndexEntry entry, List<Chunk> chunks)
	{
		try
		{
			// Get the file id
			int fileId = getFileId(log);
			if(fileId == -1)
				return false;
			
			int index = log.getSessions().indexOf(entry);
			if(index == -1)
				return false;
			LogUtil.finer("Adding session to reference");
			mAddSessionStatement.setInt(1, fileId);
			mAddSessionStatement.setInt(2, index);
			mAddSessionStatement.setLong(3, entry.StartTimestamp);
			mAddSessionStatement.setLong(4, entry.EndTimestamp);
			
			mAddSessionStatement.executeUpdate();
			
			LogUtil.finer("Finding Session ID");
			// Find the session id
			mSelectSessionStatement.setInt(1, fileId);
			mSelectSessionStatement.setInt(2, index);
			ResultSet rs = mSelectSessionStatement.executeQuery();
			if(!rs.next())
				return false;
			int sessionId = rs.getInt(1); 
			LogUtil.finer("Session ID = " + sessionId);
			
			mAddChunkStatement.clearBatch();
			int count = 0;
			for(Chunk chunk : chunks)
			{
				if(chunk == null)
					continue;
				count++;
				mAddChunkStatement.setInt(1, chunk.getX());
				mAddChunkStatement.setInt(2, chunk.getZ());
				mAddChunkStatement.setInt(3, getWorldHash(chunk.getWorld()));
				mAddChunkStatement.setInt(4, sessionId);
				mAddChunkStatement.addBatch();
			}
			LogUtil.finer("Adding " + count + " Chunks");
			mAddChunkStatement.executeBatch();
			
			mDatabaseConnection.commit();
			return true;
		}
		catch(SQLException e)
		{
			LogUtil.severe(e.getMessage());
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
			
			int index = log.getSessions().indexOf(entry);
			
			if(index == -1)
				return false;
			
			// Find the session id
			mSelectSessionStatement.setInt(1, fileId);
			mSelectSessionStatement.setInt(2, index);
			ResultSet rs = mSelectSessionStatement.executeQuery();
			
			if(!rs.next())
				return false;
			int sessionId = rs.getInt(1); 
			
			mDeleteSessionStatement.setInt(1, sessionId);
			mDeleteSessionStatement.execute();
			
			mDatabaseConnection.commit();
			return true;
		}
		catch(SQLException e)
		{
			LogUtil.severe(e.getMessage());
			return false;
		}
	}
	/**
	 * Updates the session information
	 * @param log The log file the session is for
	 * @param entry The session
	 * @return True if the update was successful
	 */
	public synchronized boolean updateSession(LogFile log, IndexEntry entry, List<Chunk> chunks)
	{
		try
		{
			
			int fileId = getFileId(log);
			int index = log.getSessions().indexOf(entry);
			
			LogUtil.finer("Finding Session ID");
			// Find the session id
			mSelectSessionStatement.setInt(1, fileId);
			mSelectSessionStatement.setInt(2, index);
			ResultSet rs = mSelectSessionStatement.executeQuery();
			
			if(!rs.next())
				return false;
			int sessionId = rs.getInt(1);
			
			LogUtil.finer("Session Id = " + sessionId);
			LogUtil.finer("Updating Basic Session Info");
			mUpdateSessionStatement.setInt(1, index);
			mUpdateSessionStatement.setLong(2, entry.StartTimestamp);
			mUpdateSessionStatement.setLong(3, entry.EndTimestamp);
			mUpdateSessionStatement.setInt(4, sessionId);
			
			mUpdateSessionStatement.executeUpdate();
			
			// Find what chunks are new
			LogUtil.finer("Finding New Chunks");
			mSelectChunksBySessionStatement.setInt(1, sessionId);
			ResultSet existingChunks = mSelectChunksBySessionStatement.executeQuery();

			while(existingChunks.next())
			{
				// Columns are: X, Z, WORLD, SESSION_ID
				for(Chunk chunk : chunks)
				{
					if(chunk.getX() == existingChunks.getInt(1) && chunk.getZ() == existingChunks.getInt(2) && getWorldHash(chunk.getWorld()) == existingChunks.getInt(3))
					{
						chunks.remove(chunk);
						break;
					}
				}
			}
			
			// Add the chunks
			mAddChunkStatement.clearBatch();
			int count = 0;
			for(Chunk chunk : chunks)
			{
				count++;
				mAddChunkStatement.setInt(1, chunk.getX());
				mAddChunkStatement.setInt(2, chunk.getZ());
				mAddChunkStatement.setInt(3, getWorldHash(chunk.getWorld()));
				mAddChunkStatement.setInt(4, sessionId);
				mAddChunkStatement.addBatch();
			}
			LogUtil.finer("Adding " + count + " new chunks");
			mAddChunkStatement.executeBatch();
			
			
			mDatabaseConnection.commit();
			return true;
		}
		catch(SQLException e)
		{
			SQLException ex = e;
			do
			{
				LogUtil.severe(ex.getMessage());
				
			} while((ex = ex.getNextException()) != null);
			
			return false;
		}
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
		try
		{
			int fileId = getFileId(log);
			
			// Find the session id
			mSelectSessionStatement.setInt(1, fileId);
			mSelectSessionStatement.setInt(2, oldIndex);
			ResultSet rs = mSelectSessionStatement.executeQuery();
			if(!rs.next())
				return false;
			int sessionId = rs.getInt(1); 
			
			mUpdateSessionIndexStatement.setInt(1,newIndex);
			mUpdateSessionIndexStatement.setInt(2,sessionId);
			mUpdateSessionIndexStatement.executeUpdate();
			
			mDatabaseConnection.commit();
			return true;
		}
		catch(SQLException e)
		{
			LogUtil.severe(e.getMessage());
			return false;
		}
	}
	/**
	 * Gets all the sessions that contain that chunk
	 * @param chunk The chunk to check for
	 * @return A list of SessionInFile objects that contain the session and the logfile. You should call releaseLastLogs() when you are done with the results
	 */
	public synchronized List<SessionInFile> getSessionsFor(Chunk chunk)
	{
		try
		{
			mSelectSessionByChunkStatement.setInt(1, chunk.getX());
			mSelectSessionByChunkStatement.setInt(2, chunk.getZ());
			mSelectSessionByChunkStatement.setInt(3, getWorldHash(chunk.getWorld()));
			
			ResultSet rs = mSelectSessionByChunkStatement.executeQuery();
			ArrayList<SessionInFile> results = new ArrayList<CrossReferenceIndex.SessionInFile>();
			releaseLastLogs();
			
			while(rs.next())
			{
				// Columns are: SESSION_ID, FILE_ID, SESSION_INDEX, START_DATE, END_DATE
				int fileId = rs.getInt(2);
				LogFile log = null;
				if(mOpenedLogs.containsKey(fileId))
				{
					log = mOpenedLogs.get(fileId);
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
					
					String playerName = fileRs.getString(2);
					if(playerName.equals("__global"))
						log = LogFileRegistry.getGlobalLog();
					else
					{
						OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
						log = LogFileRegistry.getLogFile(player);
					}
					fileRs.close();
					
					mOpenedLogs.put(fileId, log);
				}
				
				// Because inconistant state can happen though crashes. Just good to make sure
				if(log.getSessions().size() > rs.getInt(3))
				{
					SessionInFile res = new SessionInFile();
					res.Log = log;
					res.Session = log.getSessions().get(rs.getInt(3));
					results.add(res);
				}
			}
			
			rs.close();
			
			return results;
		}
		catch(SQLException e)
		{
			LogUtil.severe(e.getMessage());
			return new ArrayList<CrossReferenceIndex.SessionInFile>();
		}
	}
	/**
	 * Gets all the session that contain that chunk and are within the time limit
	 * @param chunk The chunk to check for
	 * @param startTime The earilist date you wish to check for
	 * @param endTime The latest date you with to check for
	 * @return A list of SessionInFile objects that contain the session and the logfile. You should call releaseLastLogs() when you are done with the results 
	 */
	public synchronized List<SessionInFile> getSessionsFor(Chunk chunk, long startTime, long endTime)
	{
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
