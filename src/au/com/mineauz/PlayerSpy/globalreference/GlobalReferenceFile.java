package au.com.mineauz.PlayerSpy.globalreference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;

import com.google.common.collect.Multimap;

import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.Utilities.ACIDRandomAccessFile;
import au.com.mineauz.PlayerSpy.Utilities.CubicChunk;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.structurefile.Index;
import au.com.mineauz.PlayerSpy.structurefile.SpaceLocator;
import au.com.mineauz.PlayerSpy.structurefile.StructuredFile;
import au.com.mineauz.PlayerSpy.tracdata.FileHeader;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;

public class GlobalReferenceFile extends StructuredFile
{
	private boolean mIsLoaded;
	
	private GRFileHeader mHeader;
	
	private SpaceLocator mSpaceLocator;
	
	private HoleIndex mHoleIndex;
	private FileIndex mFileIndex;
	private SessionIndex mSessionIndex;
	private ChunkIndex mChunkIndex;
	
	public GlobalReferenceFile()
	{
		super();
		
		mIsLoaded = false;
	}
	
	public boolean isLoaded()
	{
		return mIsLoaded;
	}
	
	public static GlobalReferenceFile create(File path)
	{
		ACIDRandomAccessFile file = null;
		try 
		{
			file = new ACIDRandomAccessFile(path, "rw");
		}
		catch (FileNotFoundException e) 
		{
			return null;
		}
		
		
		// Write the file header
		GRFileHeader header = new GRFileHeader();
		
		header.HolesIndexLocation = header.getSize();
		header.HolesIndexSize = 0;
		header.HolesIndexCount = 0;
		header.HolesIndexPadding = 0;
		
		header.SessionIndexLocation = header.getSize();
		header.SessionIndexSize = 0;
		header.SessionIndexCount = 0;
		
		header.FileIndexLocation = header.getSize();
		header.FileIndexSize = 0;
		header.FileIndexCount = 0;
		
		header.ChunkIndexLocation = header.getSize();
		header.ChunkIndexSize = 0;
		header.ChunkIndexCount = 0;
		
		try
		{
			// Clean it first
			file.setLength(0);
			file.beginTransaction();
			
			header.write(file);
			
			file.commit();
		}
		catch(IOException e)
		{
			LogUtil.severe("Failed to create global reference file");
			e.printStackTrace();
			
			Debug.severe("Failed to create global reference file");
			Debug.logException(e);
			
			file.rollback();
			
			return null;
		}
		
		// initialize the logfile instance
		
		GlobalReferenceFile reference = new GlobalReferenceFile();
		
		reference.mSpaceLocator = new SpaceLocator(reference);
		reference.mHoleIndex = new HoleIndex(reference, header, file, reference.mSpaceLocator);
		reference.mSpaceLocator.setHoleIndex(reference.mHoleIndex);
		
		reference.mSessionIndex = new SessionIndex(reference, header, file, reference.mSpaceLocator);
		reference.mFileIndex = new FileIndex(reference, header, file, reference.mSpaceLocator);
		
		if(header.VersionMajor >= 2)
		{
			reference.mChunkIndex = new ChunkIndex(reference, header, file, reference.mSpaceLocator);
			reference.load(file, path, new Index[] {reference.mHoleIndex, reference.mSessionIndex, reference.mFileIndex, reference.mChunkIndex});
		}
		else
			reference.load(file, path, new Index[] {reference.mHoleIndex, reference.mSessionIndex, reference.mFileIndex});
		
		reference.mIsLoaded = true;
		reference.mHeader = header;
		
		Debug.fine("Created global reference file");
		
		return reference;
	}
	
	public boolean load(File filePath)
	{
		Debug.loggedAssert(!mIsLoaded);
		
		boolean ok = false;
		lockWrite();
		ACIDRandomAccessFile file = null;

		try
		{
			Debug.info("Loading '" + filePath.getPath() + "'...");
			file = new ACIDRandomAccessFile(filePath, "rw");
			
			// Read the file header
			GRFileHeader header = new GRFileHeader();
			header.read(file);
			
			mHeader = header;
			
			// Initialize the indexes
			mSpaceLocator = new SpaceLocator(this);
			mHoleIndex = new HoleIndex(this, mHeader, file, mSpaceLocator);
			mSpaceLocator.setHoleIndex(mHoleIndex);
			
			mSessionIndex = new SessionIndex(this, mHeader, file, mSpaceLocator);
			mFileIndex = new FileIndex(this, mHeader, file, mSpaceLocator);
			
			if(header.VersionMajor >= 2)
			{
				mChunkIndex = new ChunkIndex(this, mHeader, file, mSpaceLocator);
				load(file, filePath, new Index[] {mHoleIndex, mSessionIndex, mFileIndex, mChunkIndex});
			}
			else
				load(file, filePath, new Index[] {mHoleIndex, mSessionIndex, mFileIndex});
			
			
			// Read the indices
			mHoleIndex.read();
			mSessionIndex.read();
			mFileIndex.read();
			
			if(header.VersionMajor >= 2)
				mChunkIndex.read();
			
			Debug.info("Global reference load Succeeded:");
			Debug.fine(" Files: " + mFileIndex.getCount());
			Debug.fine(" Sessions: " + mSessionIndex.getCount());
			Debug.fine(" Holes: " + mHoleIndex.getCount());
			if(header.VersionMajor >= 2)
				Debug.fine(" Chunks: " + mChunkIndex.getCount());
			
			mIsLoaded = true;
			ok = true;
		}
		catch(IOException e)
		{
			Debug.logException(e);
			try
			{
				if(file != null)
					file.close();
			}
			catch(IOException ex)
			{
				Debug.logException(ex);
			}
			ok = false;
		}
		catch(Exception e)
		{
			Debug.logException(e);
			try
			{
				if(file != null)
					file.close();
			}
			catch(IOException ex)
			{
				Debug.logException(ex);
			}
			ok = false;
		}
		
		unlockWrite();
		
		return ok;
	}
	
	public void close()
	{
		try
		{
			mFile.close();
		}
		catch(IOException e)
		{
			Debug.logException(e);
		}
	}
	
	private UUID getFileID(LogFile log) throws IOException
	{
		FileEntry entry = mFileIndex.get(log.getName());
		if(entry != null)
			return entry.fileId;
		
		// Add a new entry
		entry = new FileEntry();
		entry.fileId = UUID.randomUUID();
		entry.fileName = log.getName();
		entry.timeBegin = log.getStartDate();
		entry.timeEnd = log.getEndDate();
		
		mFileIndex.add(entry);
		
		return entry.fileId;
	}
	
	private UUID getFileID(String logName)
	{
		FileEntry entry = mFileIndex.get(logName);
		if(entry == null)
			return null;
		
		return entry.fileId;
	}
	
	public String getFileName(UUID fileId)
	{
		FileEntry entry = mFileIndex.get(fileId);
		if(entry == null)
			return null;
		
		return entry.fileName;
	}
	
	public void addSession(au.com.mineauz.PlayerSpy.tracdata.SessionEntry session, LogFile log, Set<CubicChunk> chunks) throws IOException
	{
		UUID fileId = getFileID(log);
		
		// Add a new session
		SessionEntry entry = new SessionEntry();
		entry.fileId = fileId;
		entry.otherBB = session.otherBB;
		entry.playerBB = session.playerBB;
		
		entry.startTime = session.StartTimestamp;
		entry.endTime = session.EndTimestamp;
		
		entry.sessionId = session.Id;
		
		mSessionIndex.add(entry);
		
		// Update the start and end dates
		FileEntry fileEntry = mFileIndex.get(fileId);
		fileEntry.timeBegin = Math.min(fileEntry.timeBegin, session.StartTimestamp);
		fileEntry.timeEnd = Math.max(fileEntry.timeEnd, session.EndTimestamp);
		
		mFileIndex.set(fileId, fileEntry);
		
		HashSet<Long> visitedChunks = new HashSet<Long>();
		for(CubicChunk chunk : chunks)
		{
			long id = chunk.chunkX | (long)chunk.chunkZ << 32;
			
			if(visitedChunks.contains(id))
				continue;
			
			mChunkIndex.addSessionToChunk(chunk.chunkX, chunk.chunkZ, chunk.worldHash, fileId, session.Id);
			visitedChunks.add(id);
		}
	}
	
	public void updateSession(au.com.mineauz.PlayerSpy.tracdata.SessionEntry session, LogFile log, Set<CubicChunk> chunks) throws IOException
	{
		UUID fileId = getFileID(log);
		
		SessionEntry entry = mSessionIndex.get(fileId, session.Id);
		
		if(entry == null)
		{
			addSession(session, log, chunks);
			return;
		}
		
		entry.otherBB = session.otherBB;
		entry.playerBB = session.playerBB;
		
		entry.startTime = session.StartTimestamp;
		entry.endTime = session.EndTimestamp;
		
		mSessionIndex.set(mSessionIndex.indexOf(entry), entry);
		
		// Update the start and end dates for the file
		FileEntry fileEntry = mFileIndex.get(fileId);
		fileEntry.timeBegin = Math.min(fileEntry.timeBegin, session.StartTimestamp);
		fileEntry.timeEnd = Math.max(fileEntry.timeEnd, session.EndTimestamp);
		
		mFileIndex.set(fileId, fileEntry);
		
		HashSet<Long> visitedChunks = new HashSet<Long>();
		for(CubicChunk chunk : chunks)
		{
			long id = chunk.chunkX | (long)chunk.chunkZ << 32;
			
			if(visitedChunks.contains(id))
				continue;
			
			mChunkIndex.addSessionToChunk(chunk.chunkX, chunk.chunkZ, chunk.worldHash, fileId, session.Id);
			visitedChunks.add(id);
		}
	}
	
	public void removeSession(au.com.mineauz.PlayerSpy.tracdata.SessionEntry session, LogFile log) throws IOException
	{
		UUID fileId = getFileID(log);
		
		mSessionIndex.remove(fileId, session.Id);
		
		if (mSessionIndex.getCount(fileId) == 0)
			mFileIndex.remove(fileId);
		else
		{
			// Update the start and end dates for the file
			FileEntry fileEntry = mFileIndex.get(fileId);
			
			long timeBegin = Long.MAX_VALUE;
			long timeEnd = Long.MIN_VALUE;
			
			for(SessionEntry entry : mSessionIndex.subset(fileId))
			{
				timeBegin = Math.min(timeBegin, entry.startTime);
				timeEnd = Math.max(timeEnd, entry.endTime);
			}
			fileEntry.timeBegin = timeBegin;
			fileEntry.timeEnd = timeEnd;
			
			mFileIndex.set(fileId, fileEntry);
		}
		
		Set<CubicChunk> chunks = log.getPresentChunks(session);
		
		HashSet<Long> visitedChunks = new HashSet<Long>();
		for(CubicChunk chunk : chunks)
		{
			long id = chunk.chunkX | (long)chunk.chunkZ << 32;
			
			if(visitedChunks.contains(id))
				continue;
			
			mChunkIndex.removeSessionFromChunk(chunk.chunkX, chunk.chunkZ, chunk.worldHash, fileId, session.Id);
			visitedChunks.add(id);
		}
	}
	
	public void removeLog(File logFile) throws IOException
	{
		// Attempt a scrape
		FileHeader header = LogFile.scrapeHeader(logFile.getAbsolutePath());
		
		String name;
		if(header == null)
			// Attempt to extract the name out of the path
			name = logFile.getName().substring(0, logFile.getName().lastIndexOf('.'));
		else
			name = header.PlayerName;
		
		UUID fileId = getFileID(name);
		if(fileId == null)
			throw new IllegalArgumentException("Unable to locate file in reference");
		
		// Remove all the sessions that belong to it
		for(SessionEntry entry : mSessionIndex.subset(fileId))
		{
			mSessionIndex.remove(entry);
			
			// TODO: Somehow remove the chunk entries
		}
		
		// Remove the file
		mFileIndex.remove(fileId);
	}
	public void removeLog(LogFile log) throws IOException
	{
		UUID fileId = getFileID(log);
		
		// Remove all the sessions that belong to it
		for(SessionEntry entry : mSessionIndex.subset(fileId))
		{
			Set<CubicChunk> chunks = log.getPresentChunks(entry.sessionId);
			
			HashSet<Long> visitedChunks = new HashSet<Long>();
			for(CubicChunk chunk : chunks)
			{
				long id = chunk.chunkX | (long)chunk.chunkZ << 32;
				
				if(visitedChunks.contains(id))
					continue;
				
				mChunkIndex.removeSessionFromChunk(chunk.chunkX, chunk.chunkZ, chunk.worldHash, fileId, entry.sessionId);
				visitedChunks.add(id);
			}
			
			mSessionIndex.remove(entry);
		}
		
		// Remove the file
		mFileIndex.remove(fileId);
	}
	
	public List<SessionEntry> getSessionsFor(Location location)
	{
		ArrayList<SessionEntry> sessions = new ArrayList<SessionEntry>();
		
		CubicChunk chunk = new CubicChunk(location);
		
		try
		{
			Multimap<UUID, Integer> inChunk = mChunkIndex.getSessionsInChunk(chunk.chunkX, chunk.chunkZ, chunk.worldHash);
		
			if(inChunk != null)
			{
				for(Entry<UUID, Integer> entry : inChunk.entries())
				{
					SessionEntry session = mSessionIndex.get(entry.getKey(), entry.getValue());
					
					if(session != null && session.otherBB.isContained(location))
						sessions.add(session);
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return sessions;
	}
	
	public List<SessionEntry> getSessionsBetween(long timeBegin, long timeEnd)
	{
		ArrayList<SessionEntry> sessions = new ArrayList<SessionEntry>();
		
		for(FileEntry file : mFileIndex)
		{
			// Is the specified time range within that of the file 
			if((file.timeBegin >= timeBegin && file.timeBegin <= timeEnd) || (file.timeEnd >= timeBegin && file.timeEnd <= timeEnd) ||
				(file.timeBegin < timeBegin && file.timeEnd > timeBegin) || (file.timeBegin < timeEnd && file.timeEnd > timeEnd))
			{
				for(SessionEntry session : mSessionIndex.subset(file.fileId))
				{
					// Is the specified time range within that of the session
					if((session.startTime >= timeBegin && session.startTime <= timeEnd) || (session.endTime >= timeBegin && session.endTime <= timeEnd) ||
						(session.startTime < timeBegin && session.endTime > timeBegin) || (session.startTime < timeEnd && session.endTime > timeEnd))
					{
						// Order in decending time
						boolean add = false;
						for(int i = 0; i < sessions.size(); ++i)
						{
							if(sessions.get(i).endTime < session.endTime)
							{
								sessions.add(i, session);
								add = true;
								break;
							}
						}
						
						if(!add)
							sessions.add(session);
					}
				}
			}
		}
		
		return sessions;
	}
	
	/**
	 * Gets all the sessions that contains something withing the time range within the radius of the specified location
	 * @param position The center point to check
	 * @param radius The radius to check in
	 * @param allowPlayer Allow player locations to be included
	 * @return
	 */
	public List<SessionEntry> getSessionsIn(long timeBegin, long timeEnd, Location position, double radius, boolean allowPlayer)
	{
		ArrayList<SessionEntry> sessions = new ArrayList<SessionEntry>();
		
		int minChunkX = (int)(position.getBlockX() - radius) >> 4;
		int minChunkZ = (int)(position.getBlockZ() - radius) >> 4;
		int maxChunkX = (int)(position.getBlockX() + radius) >> 4;
		int maxChunkZ = (int)(position.getBlockZ() + radius) >> 4;

		int worldHash = position.getWorld().getUID().hashCode();
		for(int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX)
		{
			for(int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ)
			{
				try
				{
					Multimap<UUID, Integer> inChunk = mChunkIndex.getSessionsInChunk(chunkX, chunkZ, worldHash);
					
					if(inChunk == null)
						continue;
					
					for(Entry<UUID, Integer> entry : inChunk.entries())
					{
						SessionEntry session = mSessionIndex.get(entry.getKey(), entry.getValue());
						
						if(session == null)
							continue;
						
						if((session.startTime >= timeBegin && session.startTime <= timeEnd) || (session.endTime >= timeBegin && session.endTime <= timeEnd) ||
						   (session.startTime < timeBegin && session.endTime > timeBegin) || (session.startTime < timeEnd && session.endTime > timeEnd))
						{
							if(session.otherBB.intersects(position, radius))
							{
								// Order in decending time
								boolean add = false;
								for(int i = 0; i < sessions.size(); ++i)
								{
									if(sessions.get(i).endTime < session.endTime)
									{
										sessions.add(i, session);
										add = true;
										break;
									}
								}
								
								if(!add)
									sessions.add(session);
							}
						}
					}
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	
		return sessions;
	}
	
	public void beginTransaction() throws IOException
	{
		mFile.beginTransaction();
	}
	
	public void commitTransaction() throws IOException
	{
		mFile.commit();
	}
	
	public void rollbackTransaction()
	{
		mFile.rollback();
	}
	
	public int getVersionMajor()
	{
		return mHeader.VersionMajor;
	}
	
	public int getVersionMinor()
	{
		return mHeader.VersionMinor;
	}
}
