package au.com.mineauz.PlayerSpy.globalreference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;

import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.Utilities.ACIDRandomAccessFile;
import au.com.mineauz.PlayerSpy.Utilities.BloomFilter;
import au.com.mineauz.PlayerSpy.Utilities.SafeChunk;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.structurefile.Index;
import au.com.mineauz.PlayerSpy.structurefile.SpaceLocator;
import au.com.mineauz.PlayerSpy.structurefile.StructuredFile;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;

public class GlobalReferenceFile extends StructuredFile
{
	private boolean mIsLoaded;
	
	private GRFileHeader mHeader;
	
	private SpaceLocator mSpaceLocator;
	
	private HoleIndex mHoleIndex;
	private FileIndex mFileIndex;
	private SessionIndex mSessionIndex;
	
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
			
			load(file, filePath, new Index[] {mHoleIndex, mSessionIndex, mFileIndex});
			
			// Read the indices
			mHoleIndex.read();
			mSessionIndex.read();
			mFileIndex.read();
			
			Debug.info("Global reference load Succeeded:");
			Debug.fine(" Files: " + mFileIndex.getCount());
			Debug.fine(" Sessions: " + mSessionIndex.getCount());
			Debug.fine(" Holes: " + mHoleIndex.getCount());
			
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
		
		entry.chunkFilter = log.getChunkFilter();
		
		mFileIndex.add(entry);
		
		return entry.fileId;
	}
	
	public void addSession(SessionEntry session, LogFile log) throws IOException
	{
		UUID fileId = getFileID(log);
		
		// Add a new session
		SessionEntry entry = new SessionEntry();
		entry.fileId = fileId;
		entry.chunkFilter = session.chunkFilter;
		entry.locationFilter = session.locationFilter;
		
		entry.startTime = session.startTime;
		entry.endTime = session.endTime;
		
		entry.sessionId = session.sessionId;
		
		mSessionIndex.add(entry);
		
		// Update the start and end dates
		FileEntry fileEntry = mFileIndex.get(fileId);
		fileEntry.timeBegin = Math.min(fileEntry.timeBegin, session.startTime);
		fileEntry.timeEnd = Math.max(fileEntry.timeEnd, session.endTime);
		fileEntry.chunkFilter = log.getChunkFilter();
		
		mFileIndex.set(fileId, fileEntry);
	}
	
	public void updateSession(SessionEntry session, LogFile log) throws IOException
	{
		UUID fileId = getFileID(log);
		
		SessionEntry entry = mSessionIndex.get(fileId, session.sessionId);
		
		entry.chunkFilter = session.chunkFilter;
		entry.locationFilter = session.locationFilter;
		
		entry.startTime = session.startTime;
		entry.endTime = session.endTime;
		
		mSessionIndex.set(mSessionIndex.indexOf(entry), entry);
		
		// Update the start and end dates for the file
		FileEntry fileEntry = mFileIndex.get(fileId);
		fileEntry.timeBegin = Math.min(fileEntry.timeBegin, session.startTime);
		fileEntry.timeEnd = Math.max(fileEntry.timeEnd, session.endTime);
		fileEntry.chunkFilter = log.getChunkFilter();
		
		mFileIndex.set(fileId, fileEntry);
	}
	
	public void removeSession(SessionEntry session, LogFile log) throws IOException
	{
		UUID fileId = getFileID(log);
		
		mSessionIndex.remove(fileId, session.sessionId);
		
		if (mSessionIndex.getCount(fileId) == 0)
			mFileIndex.remove(fileId);
		else
		{
			// Update the start and end dates for the file
			FileEntry fileEntry = mFileIndex.get(fileId);
			
			long timeBegin = Long.MAX_VALUE;
			long timeEnd = Long.MIN_VALUE;
			BloomFilter chunkFilter = new BloomFilter();
			
			for(SessionEntry entry : mSessionIndex.subset(fileId))
			{
				timeBegin = Math.min(timeBegin, entry.startTime);
				timeEnd = Math.max(timeEnd, entry.endTime);
				chunkFilter.add(entry.chunkFilter);
			}
			fileEntry.timeBegin = timeBegin;
			fileEntry.timeEnd = timeEnd;
			fileEntry.chunkFilter = chunkFilter;
			
			mFileIndex.set(fileId, fileEntry);
		}
	}
	
	public void removeLog(LogFile log) throws IOException
	{
		UUID fileId = getFileID(log);
		
		// Remove all the sessions that belong to it
		for(SessionEntry entry : mSessionIndex.subset(fileId))
			mSessionIndex.remove(entry);
		
		// Remove the file
		mFileIndex.remove(fileId);
	}
	
	public List<SessionEntry> getSessionsFor(Location location)
	{
		int locationHash = location.hashCode();
		int chunkHash = new SafeChunk(location).hashCode();
		
		ArrayList<SessionEntry> sessions = new ArrayList<SessionEntry>();
		
		for(FileEntry file : mFileIndex)
		{
			if(file.chunkFilter.isPresent(chunkHash))
			{
				for(SessionEntry session : mSessionIndex.subset(file.fileId))
				{
					if(session.chunkFilter.isPresent(chunkHash) && session.locationFilter.isPresent(locationHash))
						sessions.add(session);
				}
			}
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
						sessions.add(session);
				}
			}
		}
		
		return sessions;
	}
}
