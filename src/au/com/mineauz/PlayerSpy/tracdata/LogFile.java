package au.com.mineauz.PlayerSpy.tracdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.RollbackEntry;
import au.com.mineauz.PlayerSpy.RollbackListEntry;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.LogTasks.*;
import au.com.mineauz.PlayerSpy.Records.*;
import au.com.mineauz.PlayerSpy.Utilities.ACIDRandomAccessFile;
import au.com.mineauz.PlayerSpy.Utilities.SafeChunk;
import au.com.mineauz.PlayerSpy.Utilities.Util;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.debugging.Profiler;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;

public class LogFile 
{
	//private static final ExecutorService mAsyncService = Executors.newSingleThreadExecutor(); 
	private static int NextId = (int)(System.currentTimeMillis() / 1000);
	
	public LogFile()
	{
		mReferenceCount = 1;
		mLock = new ReentrantReadWriteLock(true);
		mReferenceLock = new ReentrantLock(true);
	}

	/**
	 * Increases the reference count.
	 * Be sure to call close() when done
	 */
	public void addReference()
	{
		mReferenceLock.lock();
		
		if(mTimeoutId != -1)
		{
			Bukkit.getScheduler().cancelTask(mTimeoutId);
			mTimeoutId = -1;
		}
		mReferenceCount++;
		mReferenceLock.unlock();
		
	}
	
	public boolean isLoaded()
	{
		return mIsLoaded && !mIsClosing;
	}
	public boolean isCorrupted()
	{
		return mIsCorrupt;
	}
	public boolean isTimingOut()
	{
		return mTimeoutId != -1;
	}
	public boolean requiresOwnerTags()
	{
		return mHeader.RequiresOwnerTags;
	}
	 
	public long getStartDate()
	{
		mLock.readLock().lock();
		
		
		long result = 0;
		if(mIndex.size() > 0)
			result = mIndex.get(0).StartTimestamp;
		mLock.readLock().unlock();
		
		return result;
	}
	
	public long getEndDate()
	{
		mLock.readLock().lock();
		
		long result = 0;
		if(mIndex.size() > 0)
			result = mIndex.get(mIndex.size()-1).EndTimestamp;
		
		mLock.readLock().unlock();
		
		return result;
	}
	
	/**
	 * Creates a blank log file ready to accept sessions.
	 * It will be open when returned so remember to call close() when you're done
	 * @param playerName The name of the player this is a log of
	 * @param filename The filename to create the log at
	 * @return An instance with an open log or null if unable to create it
	 */
	public static LogFile create(String playerName, String filename)
	{
		ACIDRandomAccessFile file = null;
		try 
		{
			file = new ACIDRandomAccessFile(filename, "rw");
		}
		catch (FileNotFoundException e) 
		{
			return null;
		}
		
		
		// Write the file header
		FileHeader header = new FileHeader();
		
		header.PlayerName = playerName;
		header.HolesIndexLocation = header.getSize();
		header.HolesIndexSize = 0;
		header.HolesIndexCount = 0;
		header.HolesIndexPadding = HoleEntry.cSize;
		
		header.IndexLocation = header.getSize() + HoleEntry.cSize;
		header.IndexSize = 0;
		header.SessionCount = 0;
		
		header.OwnerMapCount = 0;
		header.OwnerMapSize = 0;
		header.OwnerMapLocation = header.getSize() + HoleEntry.cSize;
		
		header.RollbackIndexCount = 0;
		header.RollbackIndexSize = 0;
		header.RollbackIndexLocation = header.getSize() + HoleEntry.cSize;
		
		if (playerName.startsWith(LogFileRegistry.cGlobalFilePrefix))
			header.RequiresOwnerTags = true;
		else
			header.RequiresOwnerTags = false;
		
		
		try
		{
			// Clean it first
			file.setLength(0);
			file.beginTransaction();
			
			header.write(file);
			
			// write some padding in
			file.write(new byte[HoleEntry.cSize]);
			file.commit();
		}
		catch(IOException e)
		{
			LogUtil.severe("Failed to create log file for " + playerName);
			e.printStackTrace();
			
			Debug.severe("Failed to create log file for %s", playerName);
			Debug.logException(e);
			
			file.rollback();
			
			return null;
		}
		
		// initialize the logfile instance
		
		LogFile log = new LogFile();
		log.mFilePath = new File(filename);
		log.mPlayerName = playerName;
		log.mIndex = new ArrayList<IndexEntry>();
		log.mHoleIndex = new ArrayList<HoleEntry>();
		log.mOwnerTagList = new ArrayList<OwnerMapEntry>();
		log.mActiveSessions = new HashMap<String, Integer>();
		log.mRollbackEntries = new ArrayList<RollbackEntry>();
		log.mRollbackMap = new HashMap<Integer, Integer>();
		log.mIsLoaded = true;
		log.mFile = file;
		log.mHeader = header;
		log.mReferenceCount = 1;
		

		
		log.rebuildTagMap();
		log.rebuildIndexMap();
		
		Debug.fine("Created a log file for '" + playerName + "'.");
		
		CrossReferenceIndex.instance.addLogFile(log);
		return log;
	}
	/**
	 * Creates a blank log file for world global entries ready to accept sessions.
	 * It will be open when returned so remember to call close() when you're done
	 * @param the name of the world to use including the global prefix
	 * @param filename The filename to create the log at
	 * @return An instance with an open log or null if unable to create it
	 * @deprecated Use create() instead
	 */
	@Deprecated
	public static LogFile createGlobal(String worldName, String filename)
	{
		LogFile log = create(worldName, filename);
		
		log.mHeader.RequiresOwnerTags = true;

		try
		{
			log.mFile.beginTransaction();
			
			log.mFile.seek(0);
			log.mHeader.write(log.mFile);
			
			log.mFile.commit();
		}
		catch(IOException e)
		{
			log.mFile.rollback();
			e.printStackTrace();
			return null;
		}
		
		return log;
	}
	
	/**
	 * Reads the header of a log file and returns it.
	 * @param filename The filename of the log file
	 */
	public static FileHeader scrapeHeader(String filename)
	{
		RandomAccessFile file = null;
		try
		{
			file = new RandomAccessFile(new File(filename), "r");
			FileHeader header = new FileHeader();
			header.read(file);
			
			file.close();
			
			return header;
		} 
		catch (Exception e) 
		{
			Debug.logException(e);
		}
		
		return null;
	}
	/**
	 * Attempts to load the log file
	 * @param filename The filename of the log file
	 * @return If the log file loads correctly, this will return true.
	 */
	public boolean load(String filename)
	{
		Debug.loggedAssert(!mIsLoaded);
		
		boolean ok = false;
		mLock.writeLock().lock();
		ACIDRandomAccessFile file = null;
		mIsCorrupt = false;
		try
		{
			Debug.info("Loading '" + filename + "'...");
			mFilePath = new File(filename);
			file = new ACIDRandomAccessFile(filename, "rw");
			
			// Read the file header
			FileHeader header = new FileHeader();
			header.read(file);
			
			mHeader = header;
			
			// Read the index
			readIndex(file,header);
			
			// Read the holes index
			readHoles(file,header);
			
			if(header.VersionMajor >= 2)
			{
				readOwnerMap(file, header);
				rebuildTagMap();
			}
			
			if(header.VersionMajor == 3 && header.VersionMinor >= 1)
			{
				readRollbackIndex(file, header);
			}
			else
				mRollbackEntries = new ArrayList<RollbackEntry>();
			
			rebuildRollbackMap();
			rebuildIndexMap();
			
			mPlayerName = header.PlayerName;
			
			mFile = file;
			
			mActiveSessions = new HashMap<String, Integer>();
			if(mIndex.size() > 0)
			{
				for(int i = mIndex.size() - 1; i >= 0; --i)
				{
					if(mIndex.get(i).Compressed)
						continue;
					
					if(!mActiveSessions.containsKey(getOwnerTag(i)))
					{
						mActiveSessions.put(getOwnerTag(i), mIndex.get(i).Id);
					}
				}
			}
			
			Debug.info("Load Succeeded:");
			Debug.info(" Player: " + mPlayerName);
			Debug.fine(" Sessions found: " + mHeader.SessionCount);
			Debug.fine(" Holes found: " + mHeader.HolesIndexCount);
			if(mIndex.size() > 0)
			{
				Debug.fine(" Earliest Date: " + Util.dateToString(mIndex.get(0).StartTimestamp));
				Debug.fine(" Latest Date: " + Util.dateToString(mIndex.get(mIndex.size()-1).EndTimestamp));
			}
			
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
			mIsCorrupt = true;
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
			mIsCorrupt = true;
		}
		
		mLock.writeLock().unlock();
		
		return ok;
	}

	public Future<Boolean> loadAsync(String filename)
	{
		Debug.loggedAssert(!mIsLoaded);
		
		return SpyPlugin.getExecutor().submit(new LogLoadTask(this, filename));
	}
	
	/**
	 * Decreases the reference count and closes the log file if needed
	 */
	public void close(boolean noTimeout)
	{
		Debug.loggedAssert(mIsLoaded && !mIsClosing && mTimeoutId == -1);
		
		mReferenceLock.lock();
		
		mReferenceCount--;
		
		if(mReferenceCount <= 0)
		{
			mReferenceLock.unlock();
			
			if(noTimeout || sNoTimeoutOverride)
			{
				mIsClosing = true;
				try 
				{
					// Add the close task and wait for it
					//Future<?> future = mAsyncService.submit(new CloseTask());
					Future<?> future = SpyPlugin.getExecutor().submit(new CloseTask());
					future.get();
				} 
				catch (InterruptedException e1) 
				{
					Debug.logException(e1);
				} 
				catch (ExecutionException e) 
				{
					Debug.logException(e);
				}
			}
			else
			{
				mTimeoutId = Bukkit.getScheduler().scheduleSyncDelayedTask(SpyPlugin.getInstance(), new Runnable() 
				{
					@Override
					public void run() 
					{
						mIsClosing = true;
						mTimeoutId = -1;
						Debug.finer("Submitting the close task");
						//mAsyncService.submit(new CloseTask());
						SpyPlugin.getExecutor().submit(new CloseTask());
					}
				}, SpyPlugin.getSettings().logTimeout / 50L);
			}
		}
		else
		{
			mReferenceLock.unlock();
			
		}
	}
	/**
	 * Decreases the reference count and closes the log file if needed
	 * This method is Asynchronous
	 */
	public void closeAsync(boolean noTimeout)
	{
		Debug.loggedAssert(mIsLoaded && !mIsClosing && mTimeoutId == -1);
		
		mReferenceLock.lock();
		
		mReferenceCount--;
		
		if(mReferenceCount <= 0)
		{
			mReferenceLock.unlock();
			
			if(noTimeout || sNoTimeoutOverride)
			{
				mIsClosing = true;
				Debug.finer("Submitting the close task");
				//mAsyncService.submit(new CloseTask());
				SpyPlugin.getExecutor().submit(new CloseTask());
			}
			else
			{
				mTimeoutId = Bukkit.getScheduler().scheduleSyncDelayedTask(SpyPlugin.getInstance(), new Runnable() 
				{
					@Override
					public void run() 
					{
						mIsClosing = true;
						mTimeoutId = -1;
						Debug.finer("Submitting the close task");
						//mAsyncService.submit(new CloseTask());
						SpyPlugin.getExecutor().submit(new CloseTask());
					}
				}, SpyPlugin.getSettings().logTimeout / 50L);
			}
		}
		else
		{
			mReferenceLock.unlock();
			
		}
	}
	
	/**
	 * Gets all the available chunks of data
	 */
	public List<IndexEntry> getSessions()
	{
		return mIndex;
	}
	public IndexEntry getSessionById(int id)
	{
		IndexEntry result = null;
		mLock.readLock().lock();
		
		if(mIndexMap.containsKey(id))
		{
			int index = mIndexMap.get(id);
			result = mIndex.get(index);
		}
		mLock.readLock().unlock();
		
		return result;
	}
	public String getOwnerTag(IndexEntry session)
	{
		if(session.OwnerTagId == -1)
			return null;
		
		mLock.readLock().lock();
		
		Integer index = mOwnerTagMap.get(session.OwnerTagId);
		
		String result = null;
		if(index != null)
			result = mOwnerTagList.get(index).Owner;
		
		mLock.readLock().unlock();
		
		return result;
	}
	public String getOwnerTag(int sessionIndex)
	{
		return getOwnerTag(mIndex.get(sessionIndex));
	}
	
	/**
	 * Loads records between the specified dates that have the specified ownertag and returns them
	 * @param startDate The earliest date to retrieve records for
	 * @param endDate The latest date to retrieve records for
	 * @param owner The owner of the records
	 * @return The list of records whose timestamps fall between the start and end dates 
	 */
	public RecordList loadRecords(long startDate, long endDate, String owner)
	{
		Debug.loggedAssert(mIsLoaded);
		Debug.loggedAssert(mHeader.VersionMajor >= 2, "Owner tags are only suppored in version 2 and above");
		if(!mHeader.RequiresOwnerTags)
			throw new IllegalStateException("Owner tags are not enabled in this log");
		
		Profiler.beginTimingSection("loadRecords");
		RecordList allRecords = loadRecordChunks(startDate,endDate,owner);
		
		int trimStart = 0;
		int trimEnd = 0;
		
		// Now trim the records to the time period specified
		for(int i = 0; i < allRecords.size(); i++)
		{
			// Find start Trim
			if(allRecords.get(i).getTimestamp() < startDate)
				trimStart = i;
			// Find end trim
			else if(allRecords.get(i).getTimestamp() >= endDate)
			{
				trimEnd = i;
				break;
			}
		}
		
		allRecords.splitRecords(trimEnd, true);
		allRecords.splitRecords(trimStart, false);

		Debug.fine("  " + allRecords.size() + " returned records");
		Profiler.endTimingSection();
		
		return allRecords;
	}
	/**
	 * Loads records between the specified dates that have the specified ownertag and returns them 
	 * This method is Asynchronous
	 * @param startDate The earliest date to retrieve records for
	 * @param endDate The latest date to retrieve records for
	 * @param owner The owner of the records
	 * @return A Future which gives access to the list of records whose timestamps fall between the start and end dates 
	 */
	public Future<RecordList> loadRecordsAsync(long startDate, long endDate, String owner)
	{
		Debug.finer("Submitting loadRecords async task");
//		synchronized (mAsyncService)
//		{
//			return mAsyncService.submit(new LoadRecordsAsyncTask(this, startDate, endDate, false, owner));
//		}
		return SpyPlugin.getExecutor().submit(new LoadRecordsAsyncTask(this, startDate, endDate, false, owner));
	}
	/**
	 * Loads records between the specified dates and returns them
	 * @param startDate The earliest date to retrieve records for
	 * @param endDate The latest date to retrieve records for
	 * @return The list of records whose timestamps fall between the start and end dates 
	 */
	public RecordList loadRecords(long startDate, long endDate)
	{
		Debug.loggedAssert(mIsLoaded);
		
		Profiler.beginTimingSection("loadRecords");
		RecordList allRecords = loadRecordChunks(startDate, endDate);
		
		int trimStart = 0;
		int trimEnd = 0;
		
		// Now trim the records to the time period specified
		for(int i = 0; i < allRecords.size(); i++)
		{
			// Find start Trim
			if(allRecords.get(i).getTimestamp() < startDate)
				trimStart = i;
			// Find end trim
			else if(allRecords.get(i).getTimestamp() >= endDate)
			{
				trimEnd = i;
				break;
			}
		}
		
		allRecords.splitRecords(trimEnd, true);
		allRecords.splitRecords(trimStart, false);

		Debug.fine("  " + allRecords.size() + " returned records");
		Profiler.endTimingSection();
		
		return allRecords;
	}
	/**
	 * Loads records between the specified dates and returns them
	 * This method is Asynchronous
	 * @param startDate The earliest date to retrieve records for
	 * @param endDate The latest date to retrieve records for
	 * @return A Future which gives access to the list of records whose timestamps fall between the start and end dates 
	 */
	public Future<RecordList> loadRecordsAsync(long startDate, long endDate)
	{
		Debug.finer("Submitting loadRecords async task");
//		synchronized (mAsyncService)
//		{
//			Debug.finer("Submitting loadRecords async task");
//			return mAsyncService.submit(new LoadRecordsAsyncTask(this, startDate, endDate, false));
//		}
		return SpyPlugin.getExecutor().submit(new LoadRecordsAsyncTask(this, startDate, endDate, false));
	}
	
	public long getNextAvailableDateAfter(long date)
	{
		long result = 0;
		mLock.readLock().lock();
		
		for(IndexEntry entry : mIndex)
		{
			if(entry.StartTimestamp > date && getOwnerTag(entry) == null)
			{
				result = entry.StartTimestamp;
				break;
			}
		}
		mLock.readLock().unlock();
		
		return result;
	}
	public long getNextAvailableDateBefore(long date)
	{
		long result = 0;
		mLock.readLock().lock();
		
		
		for(int i = mIndex.size()-1; i >=0 ; --i)
		{
			IndexEntry entry = mIndex.get(i);
			if(entry.EndTimestamp > date && getOwnerTag(entry) == null)
			{
				result = entry.EndTimestamp;
				break;
			}
		}
		mLock.readLock().unlock();
		
		return result;
	}
	/**
	 * Loads entire chunks of records that have have records within the start and end date that are for the specified ownertag
	 * This is more efficient than the loadRecords() method but in most cases will return records before the start date and beyond the end date. 
	 * @param startDate The earliest date you would like to retrieve records for. 
	 * @param endDate The latest date you would like to retrieve records for.
	 * @param owner The owner of the records
	 * @return The list of records
	 */
	public RecordList loadRecordChunks(long startDate, long endDate, String owner)
	{
		Debug.loggedAssert(mIsLoaded);
		Debug.loggedAssert(mHeader.VersionMajor >= 2, "Owner tags are only suppored in version 2 and above");
		if(!mHeader.RequiresOwnerTags)
			throw new IllegalStateException("Owner tags are not enabled in this log");
		
		Profiler.beginTimingSection("loadChunks");
		ArrayList<IndexEntry> relevantEntries = new ArrayList<IndexEntry>();
		
		Debug.fine("Loading records from " + Util.dateToString(startDate) + " to " + Util.dateToString(endDate));
		
		// We will hold the write lock because accessing the file concurrently through the same object with have issues i think.
		mLock.writeLock().lock();
		
				
		// Find the relevant sessions
		for(IndexEntry entry : mIndex)
		{
			if((startDate >= entry.StartTimestamp && startDate <= entry.EndTimestamp) ||
				(endDate >= entry.StartTimestamp && endDate <= entry.EndTimestamp) ||
				(entry.StartTimestamp >= startDate && entry.StartTimestamp < endDate) ||
				(entry.EndTimestamp > startDate && entry.EndTimestamp < endDate))
			{
				if(owner.equalsIgnoreCase(getOwnerTag(entry)))
				{
					relevantEntries.add(entry);
					break;
				}
					
			}
			
		}
		Debug.finer("  " + relevantEntries.size() + " Matching Sessions");
		// Now load up the records
		RecordList allRecords = new RecordList();
		for(IndexEntry session : relevantEntries)
		{
			allRecords.addAll(loadSession(session));
		}
		
		mLock.writeLock().unlock();
		Profiler.endTimingSection();
		
		Debug.finer("  " + allRecords.size() + " loaded records");

		return allRecords;
	}
	/**
	 * Loads entire chunks of records that have have records within the start and end date that are for the specified ownertag
	 * This is more efficient than the loadRecords() method but in most cases will return records before the start date and beyond the end date.
	 * This method is Asynchronous. 
	 * @param startDate The earliest date you would like to retrieve records for. 
	 * @param endDate The latest date you would like to retrieve records for.
	 * @param owner The owner of the records
	 * @return A future which gives you access to the records when they have been retrieved
	 */
	public Future<RecordList> loadRecordChunksAsync(long startDate, long endDate, String owner)
	{
		Debug.finer("Submitting loadRecordChunks async task");
//		synchronized (mAsyncService)
//		{
//			Debug.finer("Submitting loadRecordChunks async task");
//			return mAsyncService.submit(new LoadRecordsAsyncTask(this, startDate, endDate, true, owner));
//		}
		return SpyPlugin.getExecutor().submit(new LoadRecordsAsyncTask(this, startDate, endDate, true, owner));
	}
	/**
	 * Loads entire chunks of records that have have records within the start and end date
	 * This is more efficient than the loadRecords() method but in most cases will return records before the start date and beyond the end date. 
	 * @param startDate The earliest date you would like to retrieve records for. 
	 * @param endDate The latest date you would like to retrieve records for.
	 * @return The list of records
	 */
	public RecordList loadRecordChunks(long startDate, long endDate)
	{
		Debug.loggedAssert(mIsLoaded);
		Profiler.beginTimingSection("loadChunks");
		Debug.fine("Loading chunks from " + Util.dateToString(startDate) + " to " + Util.dateToString(endDate));
		ArrayList<IndexEntry> relevantEntries = new ArrayList<IndexEntry>();
		
		// We will hold the write lock because accessing the file concurrently through the same object with have issues i think.
		mLock.writeLock().lock();
					
		
		// Find the relevant sessions
		for(IndexEntry entry : mIndex)
		{
			if((startDate >= entry.StartTimestamp && startDate <= entry.EndTimestamp) ||
			   (endDate >= entry.StartTimestamp && endDate <= entry.EndTimestamp) ||
			   (entry.StartTimestamp >= startDate && entry.StartTimestamp < endDate) ||
			   (entry.EndTimestamp > startDate && entry.EndTimestamp < endDate))
			{
				if(getOwnerTag(entry) == null)
					relevantEntries.add(entry);
			}
		}
		Debug.finer("  " + relevantEntries.size() + " Matching Sessions");
		// Now load up the records
		RecordList allRecords = new RecordList();
		for(IndexEntry session : relevantEntries)
		{
			allRecords.addAll(loadSession(session));
		}
		Debug.fine("  " + allRecords.size() + " loaded records");
		
		mLock.writeLock().unlock();
		Profiler.endTimingSection();
		
		// No need to trim it
		return allRecords;
	}
	/**
	 * Loads entire chunks of records that have have records within the start and end date
	 * This is more efficient than the loadRecords() method but in most cases will return records before the start date and beyond the end date.
	 * This method is Asynchronous. 
	 * @param startDate The earliest date you would like to retrieve records for. 
	 * @param endDate The latest date you would like to retrieve records for.
	 * @return A future which gives you access to the records when they have been retrieved
	 */
	public Future<RecordList> loadRecordChunksAsync(long startDate, long endDate)
	{
		Debug.finer("Submitting loadRecordChunks async task");
//		synchronized (mAsyncService)
//		{
//			Debug.finer("Submitting loadRecordChunks async task");
//			return mAsyncService.submit(new LoadRecordsAsyncTask(this, startDate, endDate, true));
//		}
		return SpyPlugin.getExecutor().submit(new LoadRecordsAsyncTask(this, startDate, endDate, true));
	}
	public RecordList loadSession(IndexEntry session)
	{
		Debug.loggedAssert(mIsLoaded);
		
		RecordList records = null;
		Profiler.beginTimingSection("loadSession");
		
		boolean isAbsolute = getOwnerTag(session) != null;
		
		// We will hold the write lock because accessing the file concurrently through the same object with have issues i think.
		mLock.writeLock().lock();
		
		Debug.fine("Loading Session %d from %s", session.Id, mPlayerName);
		
		try
		{
			records = new RecordList();
			
			// Read the raw session data
			mFile.seek(session.Location);

			byte[] sessionRaw = new byte[(int)session.TotalSize];
			mFile.read(sessionRaw);
			
			// make it available to use
			ByteArrayInputStream istream = new ByteArrayInputStream(sessionRaw);
			
			DataInputStream stream = null;
			
			// get the input stream 
			if(session.Compressed)
			{
				GZIPInputStream compressedInput = new GZIPInputStream(istream);
				stream = new DataInputStream(compressedInput);
			}
			else
				stream = new DataInputStream(istream);
			
			World lastWorld = null;
			boolean hadInv = false;
			// Load the records
			for(int i = 0; i < session.RecordCount; i++)
			{
				Record record = Record.readRecord(stream, lastWorld, mHeader.VersionMajor, isAbsolute);
				if(record == null)
				{
					if(i == 0)
						Debug.severe("Record read fail at index 0");
					else
						Debug.severe("Record read fail at index %d after record type %s", i, records.get(records.size()-1).getClass().getName());
					
					break;
				}
				record.sourceFile = this; 
				record.sourceEntry = session;
				record.sourceIndex = (short)i;
				
				// update the last world
				if(record instanceof IPlayerLocationAware && ((IPlayerLocationAware)record).isFullLocation())
					lastWorld = ((IPlayerLocationAware)record).getLocation().getWorld();
				else if(record instanceof WorldChangeRecord)
					lastWorld = ((WorldChangeRecord)record).getWorld();
				else if((lastWorld == null && record.getType() != RecordType.FullInventory && record.getType() != RecordType.EndOfSession) && !isAbsolute)
				{
					Debug.warning("Corruption in " + mPlayerName + ".tracdata session " +mIndex.indexOf(session) + " found. Attempting to fix");
					lastWorld = Bukkit.getWorlds().get(0);
					Record worldRecord = new WorldChangeRecord(lastWorld);
					worldRecord.setTimestamp(record.getTimestamp());
					records.add(worldRecord);
					
					// Ditch the first record since it is useless
					continue;
				}
				
				if(record.getType() == RecordType.FullInventory)
					hadInv = true;
				
				if(lastWorld == null && i > 3 && !isAbsolute)
				{
					Debug.warning("Issue detected with " + mPlayerName + ".trackdata in session " + mIndex.indexOf(session) + ". No world has been set. Defaulting to main world");
					lastWorld = Bukkit.getWorlds().get(0);
					records.add(new WorldChangeRecord(lastWorld));
				}
				if(!hadInv && i > 3 && !isAbsolute)
				{
					Debug.warning("Issue detected with " + mPlayerName + ".trackdata in session " + mIndex.indexOf(session) + ". No inventory state has been set. ");
					hadInv = true;
				}
				records.add(record);
			}
			
			// Load the rollback state info in
			short[] indices = getRolledBackRecords(session);
			for(int i = 0; i < indices.length; ++i)
			{
				Record record = records.get(indices[i]);
				if(record instanceof IRollbackable)
				{
					IRollbackable r = (IRollbackable)record;
					r.setRollbackState(true);
				}
			}
			
		}
		catch(IOException e)
		{
			Debug.logException(e);
		}
		mLock.writeLock().unlock();
		
		Profiler.endTimingSection();
		
		return records;
	}
	
	/**
	 * Appends the records to the specified session. If they dont all fit, the remaining will be returned
	 * @param records The records to append
	 * @param session The session to append them to
	 * @return Null if there are no more records to add, otherwise a list of the remaining ones
	 */
	private RecordList appendRecords(RecordList records, IndexEntry session) throws IOException
	{
		Debug.info("Begining append of %d records to Session %d", records.size(), session.Id);
		
		// This will be populated if there are too many records to put into this session
		RecordList splitSession = null;
		boolean isAbsolute = getOwnerTag(session) != null;
		
		Profiler.beginTimingSection("appendRecordsInternal");
		mLock.writeLock().lock();
		
		ArrayList<Short> rolledBackEntries = new ArrayList<Short>();
		
		try
		{
			// Calculate the amount of space there is availble to extend into
			long availableSpace = 0;
			int hole = -1;
			// First find the adjacent hole
			for(int i = 0; i < mHoleIndex.size(); i++)
			{
				HoleEntry holeEntry = mHoleIndex.get(i);
				if(holeEntry.Location == session.Location + session.TotalSize)
				{
					if(holeEntry.AttachedTo == null || holeEntry.AttachedTo == session)
					{
						availableSpace = holeEntry.Size;
						hole = i;
					}
					break;
				}
			}
			// Check if this is at the end of the file
			if(session.Location + session.TotalSize == mFile.length())
				availableSpace = Long.MAX_VALUE;
			
			Debug.fine("*Avaiable Space: " + availableSpace);
			
			// Calculate the size of the records
			long totalSize = 0;
			int cutoffIndex = 0;
			
			if(!session.Compressed)
			{
				short index = 0;
				for(Record record : records)
				{
					int size = record.getSize(isAbsolute);
					
					if(totalSize + size > availableSpace)
					{
						// Split
						splitSession = records.splitRecords(cutoffIndex, true);
						break;
					}
					totalSize += size;
					cutoffIndex++;
					
					// Update deep mode
					if(record instanceof SessionInfoRecord)
					{
						mDeepMode = ((SessionInfoRecord)record).isDeep();
					}
					
					if(record instanceof IRollbackable)
					{
						if(((IRollbackable)record).wasRolledBack())
							rolledBackEntries.add((Short)(short)(index + session.RecordCount));
					}
					++index;
				}
			}
			else
			{
				Debug.warning("Attempting to write to compressed session. Moving to new session");
				splitSession = records.splitRecords(cutoffIndex, true);
			}
			
			Debug.fine("*Total size to write: " + totalSize);
			if(splitSession != null)
				Debug.fine("*Cutoff at: " + cutoffIndex);
			
			if(records.size() != 0)
			{
				totalSize = records.getDataSize(isAbsolute);
				// Encode the records
				ByteArrayOutputStream bstream = new ByteArrayOutputStream((int)totalSize);
				DataOutputStream dstream = new DataOutputStream(bstream);
				long lastSize = dstream.size();
				for(int i = 0; i < records.size(); i++)
				{
					int expectedSize = records.get(i).getSize(isAbsolute);
					records.get(i).write(dstream, isAbsolute);
					
					long actualSize = dstream.size() - lastSize;
					
					if(expectedSize != actualSize)
					{
						Debug.severe(records.get(i).getType().toString() + " is returning incorrect size. Expected: " + expectedSize + " got " + actualSize);
					}
					lastSize = dstream.size();
				}
		
				// ensure i havent messed up the implementation of getSize()
				Debug.loggedAssert(totalSize == bstream.size(), "Get size returned bad size");
				
				// Write it into the file
				mFile.seek(session.Location + session.TotalSize);
				Debug.finest("*Writing from %X -> %X", session.Location + session.TotalSize, session.Location + session.TotalSize + bstream.size()-1);
				
				mFile.write(bstream.toByteArray());
	
				// Consume the space
				if(hole != -1)
					fillHole(hole,session.Location + session.TotalSize,bstream.size());
				
				// Update the session info
				session.TotalSize += bstream.size();
				session.EndTimestamp = records.getEndTimestamp();
				session.RecordCount += records.size();
				updateSession(mIndex.indexOf(session), session);
				
				CrossReferenceIndex.instance.updateSession(this, session, records.getAllChunks());
				Debug.info("Completed append to Session %d", session.Id);
				
				if(!rolledBackEntries.isEmpty())
					setRollbackStateInternal(session, rolledBackEntries, true);
			}
			
			if(splitSession != null && !session.Compressed)
			{
				// Finalize the old session by compressing it
				Debug.info("Found more records to write in new session. Compressing session");
				
				byte[] sessionData = new byte[(int)session.TotalSize];
				mFile.seek(session.Location);
				mFile.readFully(sessionData);
				
				ByteArrayOutputStream ostream = new ByteArrayOutputStream();
				GZIPOutputStream compressor = new GZIPOutputStream(ostream);
				
				compressor.write(sessionData);
				compressor.finish();
				
				if(ostream.size() < session.TotalSize)
				{
					Debug.fine("Compressed to %d from %d. Reduction of %.1f%%", ostream.size(), session.TotalSize, (session.TotalSize-ostream.size()) / (double)session.TotalSize * 100F);
					
					HoleEntry freedSpace = new HoleEntry();
					freedSpace.Location = session.Location + ostream.size();
					freedSpace.Size = session.TotalSize - ostream.size();
					freedSpace.AttachedTo = null;
					
					mFile.seek(session.Location);
					mFile.write(ostream.toByteArray());
					
					session.TotalSize = ostream.size();
					session.Compressed = true;
					
					updateSession(mIndex.indexOf(session), session);
					addHole(freedSpace);
					
					if(hole != -1)
						mHoleIndex.get(hole).AttachedTo = null;
					
					pullData(freedSpace.Location);
				}
				else
					Debug.fine("Compression cancelled as the result was larger than the original");
				
			}
		}
		catch (Exception e)
		{
			Debug.logException(e);
			throw e;
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}

		return splitSession;
	}
	
	/**
	 * Appends records onto a matching session for a particular ownertag.
	 * If there is no session available, a new session will be created
	 * @param records The list of records to append
	 * @return True if append was successful
	 * @throws IllegalStateException if owner tags are not enabled in the log file. You must use the other version of appendRecords()
	 */
	public boolean appendRecords(RecordList records, String owner)
	{
		Debug.loggedAssert(mIsLoaded);
		Debug.loggedAssert(mHeader.VersionMajor >= 2, "Owner tags are only suppored in version 2 and above");
		if(mOwnerTagList == null)
		{
			Debug.severe("OwnerMap is null. Log: " + getName() + " Version: " + mHeader.VersionMajor + "." + mHeader.VersionMinor);
			return false;
		}

		boolean result;
		
		Profiler.beginTimingSection("appendRecords");
		synchronized (CrossReferenceIndex.instance)
		{
			mLock.writeLock().lock();
			
			try
			{
				mFile.beginTransaction();
				
				Debug.info("Appending " + records.size() + " records to " + mPlayerName + ">" + owner);
				
				if(!mActiveSessions.containsKey(owner) || getSessionById(mActiveSessions.get(owner)) == null)
				{
					Debug.info("No active session for %s>%s. Creating one", mPlayerName, owner);
					
					int index = initialiseSession(records, true);
					if(index != -1)
					{
						
						mActiveSessions.put(owner, mIndex.get(index).Id);
	
						IndexEntry session = mIndex.get(index);
						session.OwnerTagId = -1;
						
						// See if there is a tag we can reuse
						for(OwnerMapEntry tag : mOwnerTagList)
						{
							if(tag.Owner.equalsIgnoreCase(owner))
							{
								session.OwnerTagId = tag.Id;
								break;
							}
						}
						
						if(session.OwnerTagId == -1)
						{
							session.OwnerTagId = NextId++;
							
							// Add the tag
							OwnerMapEntry ent = new OwnerMapEntry();
							ent.Owner = owner;
							ent.Id = session.OwnerTagId;
							
							mOwnerTagMap.put(session.OwnerTagId, addOwnerMap(ent));
						}
	
						updateSession(index,session);
						
						result = true;
					}
					else
						result = false;
				}
				else
				{
					
					IndexEntry activeSession = getSessionById(mActiveSessions.get(owner));
					
					RecordList splitSession = appendRecords(records, activeSession);
		
					if(splitSession != null && splitSession.size() > 0)
					{
						int index = initialiseSession(splitSession, true);
						if(index != -1)
						{
							mActiveSessions.put(owner, mIndex.get(index).Id);
		
							IndexEntry session = mIndex.get(index);
							session.OwnerTagId = -1;
							
							// See if there is a tag we can reuse
							for(OwnerMapEntry tag : mOwnerTagList)
							{
								if(tag.Owner.equalsIgnoreCase(owner))
								{
									session.OwnerTagId = tag.Id;
									break;
								}
							}
							
							if(session.OwnerTagId == -1)
							{
								session.OwnerTagId = NextId++;
								
								// Add the tag
								OwnerMapEntry ent = new OwnerMapEntry();
								ent.Owner = owner;
								ent.Id = session.OwnerTagId;
								
								mOwnerTagMap.put(session.OwnerTagId, addOwnerMap(ent));
							}
		
							updateSession(index,session);
							
							result = true;
						}
						else
							result = false;
					}
					else
						result = true;
				}
				
				mFile.commit();
			}
			catch(Exception e)
			{
				Debug.logException(e);
				mFile.rollback();
				result = false;
			}
			finally
			{
				mLock.writeLock().unlock();
				
			}
		}
		
		Profiler.endTimingSection();
		return result;
	}
	/**
	 * Appends records onto the active session.
	 * If there is no active session, a new session will be created
	 * @param records The list of records to append
	 * @return True if append was successful
	 * @throws IllegalStateException if owner tags are required in the log file. You must use the other version of appendRecords()
	 */
	public boolean appendRecords(RecordList records)
	{
		Debug.loggedAssert(mIsLoaded);
		if(mHeader.RequiresOwnerTags && mHeader.VersionMajor >= 2)
			throw new IllegalStateException("Owner tags are required. You can only append records through appendRecords(records, tag)");
		
		if(records.isEmpty())
			return false;
		
		boolean result;
		
		Profiler.beginTimingSection("appendRecords");
		synchronized(CrossReferenceIndex.instance)
		{
			Debug.info("Appending " + records.size() + " records to " + mPlayerName);
			
			mLock.writeLock().lock();
			
			try
			{
				mFile.beginTransaction();
				
				if(!mActiveSessions.containsKey(null) || getSessionById(mActiveSessions.get(null)) == null)
				{
					Debug.finer("Tried to append records. No active session was found.");
					int index = initialiseSession(records, false);
					if(index != -1)
					{
						mActiveSessions.put(null, mIndex.get(index).Id);
						result = true;
					}
					else
						result = false;
				}
				else
				{
					IndexEntry activeSession = getSessionById(mActiveSessions.get(null));
					// This will be populated if there are too many records to put into this session
					RecordList splitSession = appendRecords(records, activeSession);

					// Ensure the consistant state
					if(records.size() > 0)
					{
						Location lastLocation = records.getCurrentLocation(records.size()-1);
						InventoryRecord lastInventory = records.getCurrentInventory(records.size()-1);
						
						if(lastLocation != null)
							mLastLocation = lastLocation;
						if(lastInventory != null)
							mLastInventory = lastInventory;
					}
					// Make sure the remaining records are written
					if(splitSession != null && splitSession.size() > 0)
					{
						int index = initialiseSession(splitSession, false);
						if(index != -1)
						{
							mActiveSessions.put(null, mIndex.get(index).Id);
							result = true;
						}
						else
							result = false;
					}
					else
						result = true;
				}
				
				mFile.commit();
			}
			catch(Exception e)
			{
				Debug.logException(e);
				mFile.rollback();
				result = false;
			}
			finally
			{
				mLock.writeLock().unlock();
				
			}
		}
		Profiler.endTimingSection();
		return result;
	}
	
	public Future<Boolean> appendRecordsAsync(RecordList records)
	{
		if(mHeader.RequiresOwnerTags && mHeader.VersionMajor >= 2)
			throw new IllegalStateException("Owner tags are required. You can only append records through appendRecords(records, tag)");
		
		Debug.finest("Submitting appendRecords async task");
		return SpyPlugin.getExecutor().submit(new AppendRecordsTask(this, records));
//		synchronized (mAsyncService)
//		{
//			Debug.finest("Submitting appendRecords async task");
//			return mAsyncService.submit(new AppendRecordsTask(this, records));
//		}
	}
	public Future<Boolean> appendRecordsAsync(RecordList records, String owner)
	{
		Debug.loggedAssert(mHeader.VersionMajor >= 2, "Owner tags are only suppored in version 2 and above");
		
		Debug.finest("Submitting appendRecords async task");
		return SpyPlugin.getExecutor().submit(new AppendRecordsTask(this, records, owner));
//		synchronized (mAsyncService)
//		{
//			Debug.finest("Submitting appendRecords async task");
//			
//			return mAsyncService.submit(new AppendRecordsTask(this, records, owner));
//		}
	}
	
	private int initialiseSession(RecordList records, boolean absolute) throws IOException
	{
		Debug.loggedAssert(mIsLoaded);
		Debug.loggedAssert(records.size() > 0);
		
		Debug.fine("Initializing New Session with " + records.size() + " records");
		Profiler.beginTimingSection("initSession");
		mLock.writeLock().lock();
		
		
		try
		{
			if(!absolute)
			{
				// Add in stuff to make it consistant
				SessionInfoRecord info = new SessionInfoRecord(mDeepMode);
				info.setTimestamp(records.get(0).getTimestamp());
				records.add(0, info);
				
				if(mLastInventory != null)
				{
					mLastInventory.setTimestamp(records.get(0).getTimestamp());
					records.add(0,mLastInventory);
				}
				
				if(mLastLocation != null)
				{
					TeleportRecord record = new TeleportRecord(mLastLocation, TeleportCause.UNKNOWN);
					record.setTimestamp(records.get(0).getTimestamp());
					records.add(0,record);
				}
			}
			
			IndexEntry session = new IndexEntry();
			session.RecordCount = (short) records.size();
	
			// Calculate the expected size
			int totalSize = 0;
			for(Record record : records)
				totalSize += record.getSize(absolute);
			
			Debug.finer(" Total size of records: " + totalSize);
			Debug.finer(" Actual session size: " + Math.max(totalSize, DesiredMaximumSessionSize));
			
			// Write the records to memory
			ByteArrayOutputStream bstream = new ByteArrayOutputStream(totalSize);
			DataOutputStream stream = new DataOutputStream(bstream);
			
			long lastSize = stream.size();
			for(Record record : records)
			{
				int expectedSize = record.getSize(absolute);
				if(!record.write(stream, absolute))
					return -1;
				
				long actualSize = stream.size() - lastSize;
				
				if(expectedSize != actualSize)
				{
					Debug.severe(record.getType().toString() + " is returning incorrect size. Expected: " + expectedSize + " got " + actualSize);
				}
				// Update deep mode
				if(record instanceof SessionInfoRecord)
				{
					mDeepMode = ((SessionInfoRecord)record).isDeep();
				}
				lastSize = stream.size();
			}
			
			// Ensure i didnt mess up the implementation of getSize()
			Debug.loggedAssert( bstream.size() == totalSize);
			
			Debug.finest(" Produced byte stream");
			// Calculate size and prepare index entry
			session.Location = 0;
			session.TotalSize = stream.size();
			session.Compressed = false;
			
			session.StartTimestamp = records.get(0).getTimestamp();
			session.EndTimestamp = records.get(records.size()-1).getTimestamp();
			
			// Find a place to put the session
			for(int i = 0; i < mHoleIndex.size(); i++)
			{
				HoleEntry hole = mHoleIndex.get(i);
				
				if(hole.Size >= Math.max(session.TotalSize,DesiredMaximumSessionSize))
				{
					session.Location = hole.Location;
					fillHole(i,session.Location,session.TotalSize);
					break;
				}
			}
		
			// Write the session
			if(session.Location == 0)
			{
				// Append to the file
				session.Location = mFile.length();
				
				Debug.finest(" Writing session %d to %X -> %X", session.Id, session.Location, session.Location + bstream.size() - 1);
				
				mFile.seek(session.Location);
				mFile.write(bstream.toByteArray());
				Debug.finest(" done");
				// Keep some space for fast appends
				if(session.TotalSize < DesiredMaximumSessionSize)
				{
					HoleEntry hole = new HoleEntry();
					
					hole.Location = mFile.getFilePointer();
					hole.Size = DesiredMaximumSessionSize - session.TotalSize;
					hole.AttachedTo = session;
					mFile.seek(hole.Location + hole.Size - 1);
					//for(long i = 0; i < hole.Size; i++)
					mFile.writeByte(0);
					
					addHole(hole);
				}
			}
			else
			{
				// Write it in the hole
				mFile.seek(session.Location);
				Debug.finest(" Writing session %d to %X -> %X", session.Id, session.Location, session.Location + bstream.size() - 1);
				mFile.write(bstream.toByteArray());
			}
			
			
			// Write the index entry
			int id = addSession(session);
			
			if(!CrossReferenceIndex.instance.addSession(this, session, records.getAllChunks()))
				Debug.warning("Failed to add session to xreference");
			else
				Debug.finer("Added session to cross reference");
			
			if(!absolute)
			{
				// Keep it consistant
				Location lastLocation = records.getCurrentLocation(records.size()-1);
				InventoryRecord lastInventory = records.getCurrentInventory(records.size()-1);
				
				if(lastLocation != null)
					mLastLocation = lastLocation;
				if(lastInventory != null)
					mLastInventory = lastInventory;
			}
			
			return id;
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}

	/**
	 * Purges all records between the fromDate inclusive, and the toDate exclusive
	 */
	public boolean purgeRecords(long fromDate, long toDate)
	{
		Debug.loggedAssert( mIsLoaded);
		
		boolean result;
		Profiler.beginTimingSection("purgeRecords");
		synchronized(CrossReferenceIndex.instance)
		{
			mLock.writeLock().lock();
			
			try
			{
				mFile.beginTransaction();
				
				Debug.info("Purging records from " + Util.dateToString(fromDate) + " to " + Util.dateToString(toDate));
				ArrayList<IndexEntry> relevantEntries = new ArrayList<IndexEntry>();
				
				// Find the relevant sessions
				for(IndexEntry entry : mIndex)
				{
					if((fromDate >= entry.StartTimestamp && fromDate <= entry.EndTimestamp) ||
					   (toDate >= entry.StartTimestamp && toDate <= entry.EndTimestamp) ||
					   (entry.StartTimestamp >= fromDate && entry.StartTimestamp < toDate) ||
					   (entry.EndTimestamp > fromDate && entry.EndTimestamp < toDate))
						relevantEntries.add(entry);
				}
				Debug.finer("  " + relevantEntries.size() + " Matching Sessions");
				
				// Purge data
				for(IndexEntry entry : relevantEntries)
				{
					String otag = getOwnerTag(entry);
					boolean isAbsolute = otag != null;
					if(entry.StartTimestamp >= fromDate && entry.EndTimestamp < toDate)
					{
						// Whole session must be purged
						int index = mIndex.indexOf(entry);
						if(index == -1)
							continue;
						
						removeSession(index);
						
						if(mActiveSessions.get(otag) != null && mActiveSessions.get(otag) == entry.Id)
							mActiveSessions.remove(otag);
	
						// Pull the proceeding data forward
						pullData(entry.Location);
						
						// Purge the owner tag if no session uses it
						int count = 0;
						for(IndexEntry session : mIndex)
						{
							if(session.Id == entry.Id)
								continue;
							if(session.OwnerTagId == entry.OwnerTagId)
								count++;
						}
						
						if(count == 0)
							removeOwnerMap(mOwnerTagMap.get(entry.OwnerTagId));
						
						// So that anything else using this object through a reference, wont do any damage
						entry.RecordCount = 0;
						entry.Location = 0;
						entry.Id = -1;
						entry.TotalSize = 0;
						entry.OwnerTagId = -1;
					}
					else
					{
						int sessionIndex = mIndex.indexOf(entry);
						if(sessionIndex == -1)
							continue;
						
						// Part of the session must be purged
						RecordList sessionData = loadSession(entry);
						int startIndex = sessionData.getNextRecordAfter(fromDate);
						int endIndex = sessionData.getLastRecordBefore(toDate);
						
						// Split the data
						RecordList lower = sessionData.splitRecords(startIndex, false);
						sessionData.splitRecords(endIndex, false);
						sessionData.addAll(0, lower);
						//sessionData.splitRecords(endIndex, true);
						//sessionData.removeBefore(startIndex);
						
						// Write back the new updated data
						if(sessionData.size() == 0)
						{
							removeSession(sessionIndex);
							
							if(mActiveSessions.get(otag) != null && mActiveSessions.get(otag) == entry.Id)
								mActiveSessions.remove(otag);
							
							// Pull the proceeding data forward
							pullData(entry.Location);
							
							// So that anything else using this object through a reference, wont do any damage
							entry.RecordCount = 0;
							entry.Location = 0;
							entry.Id = -1;
							entry.TotalSize = 0;
						}
						else
						{
							// Compile the records
							int totalSize = 0;
							for(Record record : sessionData)
								totalSize += record.getSize(isAbsolute);
							
							ByteArrayOutputStream bstream = new ByteArrayOutputStream(totalSize);
							DataOutputStream stream = new DataOutputStream(bstream);
							
							long lastSize = stream.size();
							for(Record record : sessionData)
							{
								int expectedSize = record.getSize(isAbsolute);
								if(!record.write(stream,isAbsolute))
									return false;
								
								long actualSize = stream.size() - lastSize;
								
								if(expectedSize != actualSize)
								{
									Debug.severe(record.getType().toString() + " is returning incorrect size. Expected: " + expectedSize + " got " + actualSize);
								}
								lastSize = stream.size();
							}
							
							
							// Prepare the hole
							HoleEntry hole = new HoleEntry();
							hole.Size = entry.TotalSize;
							
							// Write to file
							mFile.seek(entry.Location);
							mFile.write(bstream.toByteArray());
							
							// Update the session header
							entry.Compressed = false;
							entry.StartTimestamp = sessionData.getStartTimestamp();
							entry.EndTimestamp = sessionData.getEndTimestamp();
							entry.RecordCount = (short) sessionData.size();
							entry.TotalSize = totalSize;
							
							updateSession(sessionIndex,entry);
							CrossReferenceIndex.instance.updateSession(this, entry, new ArrayList<SafeChunk>());
							
							// Finialize the hole
							hole.Size = hole.Size - entry.TotalSize;
							hole.Location = entry.Location + entry.TotalSize;
							
							addHole(hole);
							
							// Pull the proceeding data forward
							pullData(entry.Location + entry.TotalSize);
							
						}
					}
				}
				result = true;
				
				mFile.commit();
			}
			catch (IOException e)
			{
				Debug.logException(e);
				result = false;
				mFile.rollback();
			}
			catch(Exception e)
			{
				Debug.logException(e);
				result = false;
				mFile.rollback();
			}
			finally
			{
				mLock.writeLock().unlock();
				
			}
		}
		Profiler.endTimingSection();
		return result;
	}

	
	private void pullData(long location) throws IOException
	{
		Profiler.beginTimingSection("pullData");
		// Grab what ever is next after this
		long nextLocation;
		long nextSize = 0;
		int hole = getHoleAfter(location);
		// TODO: Remove the recursion here
		if(hole != -1)
		{
			HoleEntry holeData = mHoleIndex.get(hole);
			if(holeData.AttachedTo != null)
			{
				Debug.finest("Skipping over reserved space from %X -> %X attached to %d", holeData.Location, holeData.Location + holeData.Size - 1, holeData.AttachedTo.Id);
				pullData(holeData.Location + holeData.Size);
			}
			
			// Find what data needs to be pulled
			nextLocation = holeData.Location + holeData.Size;
			int type = 0;
			int index = 0;
			
			Debug.finest("Pulling data from %X to %X", nextLocation, holeData.Location);
			
			for(IndexEntry nextSession : mIndex)
			{
				if(nextSession.Location == nextLocation)
				{
					nextSize = nextSession.TotalSize;
					type = 0;
					break;
				}
				index++;
			}
			
			if(nextSize == 0)
			{
				if(mHeader.IndexLocation == nextLocation)
				{
					nextSize = mHeader.IndexSize;
					type = 1;
				}
				else if(mHeader.HolesIndexLocation == nextLocation)
				{
					type = 2;
					nextSize = mHeader.HolesIndexSize + mHeader.HolesIndexPadding;
				}
				else if(mHeader.OwnerMapLocation == nextLocation)
				{
					nextSize = mHeader.OwnerMapSize;
					type = 3;
				}
				else if(mHeader.RollbackIndexLocation == nextLocation)
				{
					nextSize = mHeader.RollbackIndexSize;
					type = 4;
				}
				else
				{
					index = 0;
					for(RollbackEntry nextEntry : mRollbackEntries)
					{
						if(nextEntry.detailLocation == nextLocation)
						{
							nextSize = nextEntry.detailSize;
							type = 5;
							break;
						}
						index++;
					}
				}
			}
			
			
			if(nextSize != 0)
			{
				// Shift the data
				long shiftAmount = holeData.Size;
				
				byte[] buffer = new byte[1024];
				int rcount = 0;
				for(long readStart = nextLocation; readStart < nextLocation + nextSize; readStart += buffer.length)
				{
					mFile.seek(readStart);
					if(readStart + buffer.length >= nextLocation + nextSize)
					{
						rcount = (int)(nextSize - (readStart - nextLocation));
						mFile.readFully(buffer,0, rcount);
					}
					else
					{
						mFile.readFully(buffer);
						rcount = buffer.length;
					}
					
					mFile.seek(readStart - shiftAmount);
					mFile.write(buffer,0,rcount);
				}
				
				
				HoleEntry old = new HoleEntry();
				old.Location = holeData.Location + nextSize;
				old.Size = holeData.Size;
				
				// Update whatever
				switch(type)
				{
				case 0: // session
				{
					IndexEntry nextSession = mIndex.get(index);
					Debug.finest("Shifted session %d from %X -> (%X-%X)", nextSession.Id, nextSession.Location, holeData.Location, holeData.Location + nextSize - 1);
					nextSession.Location = holeData.Location;
					updateSession(index, nextSession);
					break;
				}
				case 1: // Index
					Debug.finest("Shifted index from %X -> (%X-%X)", mHeader.IndexLocation, holeData.Location, holeData.Location + nextSize - 1);
					mHeader.IndexLocation = holeData.Location;
					mFile.seek(0);
					mHeader.write(mFile);
					break;
				case 2: // Hole Index
					Debug.finest("Shifted hole index from %X -> (%X-%X)", mHeader.HolesIndexLocation, holeData.Location, holeData.Location + nextSize - 1);
					mHeader.HolesIndexLocation = holeData.Location;
					mFile.seek(0);
					mHeader.write(mFile);
					break;
				case 3: // OwnerMap
					Debug.finest("Shifted owner map from %X -> (%X-%X)", mHeader.OwnerMapLocation, holeData.Location, holeData.Location + nextSize - 1);
					mHeader.OwnerMapLocation = holeData.Location;
					mFile.seek(0);
					mHeader.write(mFile);
					break;
				case 4: // Rollback Index
					Debug.finest("Shifted rollback index from %X -> (%X-%X)", mHeader.RollbackIndexLocation, holeData.Location, holeData.Location + nextSize - 1);
					mHeader.RollbackIndexLocation = holeData.Location;
					mFile.seek(0);
					mHeader.write(mFile);
					break;
				case 5: // Rollback Detail
				{
					RollbackEntry nextEntry = mRollbackEntries.get(index);
					Debug.finest("Shifted rollback detail for %d from %X -> (%X-%X)", nextEntry.sessionId, nextEntry.detailLocation, holeData.Location, holeData.Location + nextSize - 1);
					nextEntry.detailLocation = holeData.Location;
					updateRollbackEntry(nextEntry);
					break;
				}
				}
				
				// Move the hole
				removeHole(hole);
				
				// Add in the new hole
				addHole(old);
				
				// Attempt to compact further stuff
				pullData(old.Location);
			}
			else
			{
				// Nothing to pull because there is no more data after us
				removeHole(hole);
				// Trim the file
				mFile.setLength(holeData.Location);
			}
		}
		Profiler.endTimingSection();
	}
	
	private int getHoleAfter(long location)
	{
		int i = 0;
		// Find an available hole before it
		for(HoleEntry hole : mHoleIndex)
		{
			if(hole.Location >= location)
			{
				return i; 
			}
			i++;
		}
		
		return -1;
	}
	/// Checks if there is enough room reserved for that size starting at that location
	private int isRoomFor(long size, long atLocation)
	{
		mLock.readLock().lock();
		
		long fileLen;
		int holeToUse = -1;
		
		try 
		{
			fileLen = mFile.length();
		}
		catch(IOException e) 
		{
			Debug.logException(e);
			mLock.readLock().unlock();
			
			return -1;
		}
		
		int i = 0;
		for(HoleEntry hole : mHoleIndex)
		{
			if(hole.Location <= atLocation && hole.Location + hole.Size >= atLocation)
			{
				// Check if there is enough room
				if((hole.Location + hole.Size) - (size + atLocation) >= 0)
					holeToUse = i;
				break; // There cant be 2 holes next to each other so if it doesnt fit here, it cant fit at all
			}
			i++;
		}
		
		if(holeToUse == -1 && atLocation >= fileLen) // At the end of the file
			holeToUse = mHoleIndex.size();
		
		mLock.readLock().unlock();
		
		
		return holeToUse;
	}

	private boolean canMergeHoles(HoleEntry a, HoleEntry b)
	{
		if((a.Location >= b.Location && a.Location <= b.Location + b.Size) ||
		   (a.Location + a.Size >= b.Location && a.Location + a.Size <= b.Location + b.Size))
			return true;
		
		return false;
	}
	private HoleEntry mergeHoles(HoleEntry a, HoleEntry b)
	{
		if(!canMergeHoles(a,b))
			return null;
		
		HoleEntry merged = new HoleEntry();
		if(a.Location < b.Location)
		{
			merged.Location = a.Location;
			merged.Size = Math.max((b.Location - a.Location) + b.Size, a.Size);
		}
		else
		{
			merged.Location = b.Location;
			merged.Size = Math.max(b.Size, (a.Location - b.Location) + a.Size);
		}
		
		if(a.AttachedTo != null)
			merged.AttachedTo = a.AttachedTo;
		if(b.AttachedTo != null)
			merged.AttachedTo = b.AttachedTo;
		
		return merged;
	}
	private void addHole(HoleEntry entry) throws IOException
	{
		if(entry.Size == 0)
			return;
		
		Profiler.beginTimingSection("addHole");
		mLock.writeLock().lock();
		
		
		try
		{
			// Check if we need to merge it
			for(int i = 0; i < mHoleIndex.size(); i++)
			{
				HoleEntry existing = mHoleIndex.get(i);
				if(canMergeHoles(entry,existing))
				{
					HoleEntry newHole = mergeHoles(entry,existing);
					mHoleIndex.set(i, newHole);
					
					// Write the changes
					mFile.seek(mHeader.HolesIndexLocation + i * HoleEntry.cSize);
					newHole.write(mFile);
					
					Debug.finest("Merging new hole into @%d changing range from (%X->%X) into (%X->%X)", i, existing.Location, existing.Location + existing.Size-1,newHole.Location, newHole.Location + newHole.Size-1);
					
					return;
				}
			}
			
			int index = 0;
			for(index = 0; index < mHoleIndex.size(); index++)
			{
				if(mHoleIndex.get(index).Location > entry.Location)
					break;
			}
			mHoleIndex.add(index,entry);
			
			// It must be added
			if(mHeader.HolesIndexPadding >= HoleEntry.cSize)
			{
				// there is padding availble to use 
				mFile.seek(mHeader.HolesIndexLocation + index * HoleEntry.cSize);
				// Shift the entries
				for(int i = index; i < mHoleIndex.size(); i++)
					mHoleIndex.get(i).write(mFile);
				
				mHeader.HolesIndexCount = mHoleIndex.size();
				mHeader.HolesIndexSize = mHoleIndex.size() * HoleEntry.cSize;
				mHeader.HolesIndexPadding -= HoleEntry.cSize;
				
				Debug.finest("Writing %d hole entries to %X -> %X Using padding. Remaining: %d bytes", mHoleIndex.size() - index, mHeader.HolesIndexLocation + index * HoleEntry.cSize, mHeader.HolesIndexLocation + mHeader.HolesIndexSize-1, mHeader.HolesIndexPadding);
			}
			else
			{
				// See if there is a hole we can extend into
				int hole = isRoomFor(HoleEntry.cSize,mHeader.HolesIndexLocation + mHeader.HolesIndexSize);
				
				if(hole == -1 || (hole != mHoleIndex.size() && mHoleIndex.get(hole).AttachedTo != null)) 
				{
					// There isnt a hole appended to the index
					// Relocate the holes index
					HoleEntry oldIndexHole = new HoleEntry();
					long oldLocation = mHeader.HolesIndexLocation;
					oldIndexHole.Location = mHeader.HolesIndexLocation;
					oldIndexHole.Size = mHeader.HolesIndexSize + mHeader.HolesIndexPadding;
					// Try to merge it
					boolean merged = false;
					for(int i = 0; i < mHoleIndex.size(); i++)
					{
						if(canMergeHoles(oldIndexHole,mHoleIndex.get(i)))
						{
							mHoleIndex.set(i, mergeHoles(oldIndexHole,mHoleIndex.get(i)));
							
							if(merged)
								mHoleIndex.remove(oldIndexHole);
							
							oldIndexHole = mHoleIndex.get(i);
							merged = true;
						}
					}
					
					// Add it now if it wasnt merged
					if(!merged)
					{
						int index2 = 0;
						for(index2 = 0; index2 < mHoleIndex.size(); index2++)
						{
							if(mHoleIndex.get(index2).Location > oldIndexHole.Location)
								break;
						}
						mHoleIndex.add(index2,oldIndexHole);
					}
					
					mHeader.HolesIndexLocation = mFile.length();
					
					// The total size of the hole index now including 1 extra entry as padding
					long newSize = (mHoleIndex.size() + 1) * HoleEntry.cSize;
					
					// Check all the holes to see if there is any space
					int targetHole = -1;
					for(int i = 0; i < mHoleIndex.size(); i++)
					{
						if(mHoleIndex.get(i).Size >= newSize)
						{
							targetHole = i;
							mHeader.HolesIndexLocation = mHoleIndex.get(targetHole).Location;
							break;
						}
					}
					
					// Prepare the header info
					mHeader.HolesIndexPadding = HoleEntry.cSize;
					mHeader.HolesIndexCount = mHoleIndex.size();
					mHeader.HolesIndexSize = mHoleIndex.size() * HoleEntry.cSize;
					
					// Write the items
					mFile.seek(mHeader.HolesIndexLocation);
					for(int i = 0; i < mHoleIndex.size(); i++)
						mHoleIndex.get(i).write(mFile);
					
					// Padding
					mFile.write(new byte[HoleEntry.cSize]);

					Debug.finest("Moving hole index from %X to (%X -> %X) setting %d bytes padding", oldLocation, mHeader.HolesIndexLocation, mHeader.HolesIndexLocation + mHeader.HolesIndexSize - 1, mHeader.HolesIndexPadding);
					
					if(targetHole != -1) // Found a hole big enough
						// Consume the hole
						fillHole(targetHole,mHeader.HolesIndexLocation,newSize);
				}
				else
				{
					// There was a hole to use
					mFile.seek(mHeader.HolesIndexLocation + index * HoleEntry.cSize);
					for(int i = index; i < mHoleIndex.size(); i++)
						mHoleIndex.get(i).write(mFile);

					
					mHeader.HolesIndexCount = mHoleIndex.size();
					mHeader.HolesIndexSize = mHoleIndex.size() * HoleEntry.cSize;
					
					Debug.finest("Writing %d hole entries to %X -> %X", mHoleIndex.size() - index, mHeader.HolesIndexLocation + index * HoleEntry.cSize, mHeader.HolesIndexLocation + mHeader.HolesIndexSize-1);
					
					if(hole != mHoleIndex.size())
						fillHole(hole,mFile.getFilePointer() - HoleEntry.cSize,HoleEntry.cSize);
				}
			}
			
			// Write the file header
			mFile.seek(0);
			mHeader.write(mFile);
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}
	private void fillHole(int index, long start, long size) throws IOException
	{
		Debug.loggedAssert( index >= 0 && index < mHoleIndex.size());
		
		mLock.writeLock().lock();
		
		
		try
		{
			HoleEntry hole = mHoleIndex.get(index);
			
			if(start == hole.Location)
			{
				if(size == hole.Size)
				{
					// It completely covers the hole
					removeHole(index);
				}
				else
				{
					// It covers the start of the hole
					hole.Size = (hole.Location + hole.Size) - (start + size);
					hole.Location += size;
					updateHole(index,hole);
				}
			}
			else
			{
				if(start + size == hole.Location + hole.Size)
				{
					// It covers the end of the hole
					hole.Size = (start - hole.Location);
					hole.Location = start;
					
					updateHole(index,hole);
				}
				else
				{
					// It covers the middle of the hole
					HoleEntry newHole = new HoleEntry();
					newHole.Location = start + size;
					newHole.Size = (hole.Location + hole.Size) - (start + size);
					
					hole.Size = (start - hole.Location);
					
					updateHole(index,hole);
					addHole(newHole);
				}
			}
		}
		finally
		{
			mLock.writeLock().unlock();
			
		}
	}
	private void updateHole(int index, HoleEntry entry) throws IOException
	{
		Debug.loggedAssert( index >= 0 && index < mHoleIndex.size());
		Profiler.beginTimingSection("updateHole");
		mLock.writeLock().lock();
		
		
		try
		{
			mFile.seek(mHeader.HolesIndexLocation + index * HoleEntry.cSize);
			mHoleIndex.set(index,entry);
			entry.write(mFile);
			Debug.finest("Updated hole at %X -> %X", mHeader.HolesIndexLocation + index * HoleEntry.cSize, mHeader.HolesIndexLocation + index * HoleEntry.cSize + HoleEntry.cSize - 1);
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}
	private void removeHole(int index) throws IOException
	{
		Debug.loggedAssert( index >= 0 && index < mHoleIndex.size());
		Profiler.beginTimingSection("removeHole");
		mLock.writeLock().lock();
		
		
		try
		{
			if(index != mHoleIndex.size()-1)
			{
				// shift entries
				mFile.seek(mHeader.HolesIndexLocation + index * HoleEntry.cSize);
				for(int i = index+1; i < mHoleIndex.size(); i++)
					mHoleIndex.get(i).write(mFile);
			}
			
			// Clear the last entry
			mFile.seek(mHeader.HolesIndexLocation + (mHoleIndex.size()-1) * HoleEntry.cSize);
			mFile.write(new byte[HoleEntry.cSize]);
			
			mHoleIndex.remove(index);
			// Increase the padding
			mHeader.HolesIndexPadding += HoleEntry.cSize;
			mHeader.HolesIndexCount = mHoleIndex.size();
			mHeader.HolesIndexSize = mHoleIndex.size() * HoleEntry.cSize;
			
			Debug.finer("Hole removed");
			// Write the file header
			mFile.seek(0);
			mHeader.write(mFile);
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}
	
	private int addSession(IndexEntry entry) throws IOException
	{
		int sessionIndex;
		
		Profiler.beginTimingSection("addSession");
		mLock.writeLock().lock();
		
		try
		{
			entry.Id = NextId++;
			
			// Do an ordered insert by the timestamp of the session
			int insertIndex = 0;
			for(insertIndex = 0; insertIndex < mIndex.size(); insertIndex++)
			{
				if(mIndex.get(insertIndex).StartTimestamp > entry.StartTimestamp)
					break;
			}

			mIndex.add(insertIndex,entry);
			
			// Check if there is room in the file for this
			int hole = isRoomFor(IndexEntry.cSize[mHeader.VersionMajor],mHeader.IndexLocation + mHeader.IndexSize);
			
				
			if(hole == -1 || (hole != mHoleIndex.size() && mHoleIndex.get(hole).AttachedTo != null))
			{
				// Prepare to relocate the index
				HoleEntry oldIndexHole = new HoleEntry();
				oldIndexHole.Location = mHeader.IndexLocation;
				oldIndexHole.Size = mHeader.IndexSize;
				
				// Calculate the new header values
				mHeader.IndexLocation = mFile.length();
				mHeader.SessionCount = mIndex.size();
				mHeader.IndexSize = mIndex.size() * IndexEntry.cSize[mHeader.VersionMajor];
				
				// Append the index to the back of the file
				mFile.seek(mFile.length());
				for(int i = 0; i < mIndex.size(); i++)
					mIndex.get(i).write(mHeader.VersionMajor,mFile);
				
				Debug.finest("Index relocated from %X -> (%X->%X)", oldIndexHole.Location, mHeader.IndexLocation, mHeader.IndexLocation + mHeader.IndexSize-1);
				// Add the hole info
				addHole(oldIndexHole);
			}
			else
			{
				// Calculate the new header values
				mHeader.SessionCount = mIndex.size();
				mHeader.IndexSize = mIndex.size() * IndexEntry.cSize[mHeader.VersionMajor];
				
				// Shift the index entries
				mFile.seek(mHeader.IndexLocation + insertIndex * IndexEntry.cSize[mHeader.VersionMajor]);
				for(int i = insertIndex; i < mIndex.size(); i++)
					mIndex.get(i).write(mHeader.VersionMajor, mFile);
				
				Debug.finest("Writing %d index entries from %X -> %X", mIndex.size() - insertIndex, mHeader.IndexLocation + insertIndex * IndexEntry.cSize[mHeader.VersionMajor], mHeader.IndexLocation + mHeader.IndexSize - 1);
				// Consume the hole
				if(hole != mHoleIndex.size())
					fillHole(hole,mHeader.IndexLocation + (mIndex.size()-1) * IndexEntry.cSize[mHeader.VersionMajor],IndexEntry.cSize[mHeader.VersionMajor]);
			}
			
			// Write the file header
			mFile.seek(0);
			mHeader.write(mFile);

			if(insertIndex == mIndex.size()-1)
				mIndexMap.put(entry.Id, insertIndex);
			else
				rebuildIndexMap();
			
			sessionIndex = insertIndex;
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
		
		return sessionIndex;
	}
	private void removeSession(int index) throws IOException
	{
		Debug.loggedAssert( index >= 0 && index < mIndex.size());
		
		Profiler.beginTimingSection("removeSession");
		mLock.writeLock().lock();
		
		
		try
		{
			IndexEntry removedSession = mIndex.get(index);
			CrossReferenceIndex.instance.removeSession(this, mIndex.get(index));
			
			// Shift the existing entries
			mFile.seek(mHeader.IndexLocation + index * IndexEntry.cSize[mHeader.VersionMajor]);
			for(int i = index + 1; i < mIndex.size(); i++)
				mIndex.get(i).write(mHeader.VersionMajor,mFile);
			
			// Prepare the hole
			HoleEntry hole = new HoleEntry();
			hole.Location = mFile.getFilePointer();
			hole.Size = IndexEntry.cSize[mHeader.VersionMajor];
			
			// Clear the last one
			mFile.write(new byte[IndexEntry.cSize[mHeader.VersionMajor]]);
			mIndex.remove(index);
			
			// Update the header
			mHeader.IndexSize = mIndex.size() * IndexEntry.cSize[mHeader.VersionMajor];
			mHeader.SessionCount = mIndex.size();
			
			Debug.finer("Session %d removed from %d", removedSession.Id, index);
			// Write the file header
			mFile.seek(0);
			mHeader.write(mFile);
			
			addHole(hole);

			rebuildIndexMap();
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}
	private void updateSession(int index, IndexEntry session) throws IOException
	{
		Debug.loggedAssert( index >= 0 && index < mIndex.size(), "Tried to update session " + index + "/" + mIndex.size());
		Profiler.beginTimingSection("updateSession");
		mLock.writeLock().lock();
		
		
		try
		{
			mFile.seek(mHeader.IndexLocation + index * IndexEntry.cSize[mHeader.VersionMajor]);
			mIndex.set(index,session);
			session.write(mHeader.VersionMajor,mFile);
			
			Debug.finest("Session %d(@%d) updated at %X -> %X", session.Id, index, mHeader.IndexLocation + index * IndexEntry.cSize[mHeader.VersionMajor], mHeader.IndexLocation + index * IndexEntry.cSize[mHeader.VersionMajor] + IndexEntry.cSize[mHeader.VersionMajor] - 1);
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}
	
	private int addOwnerMap(OwnerMapEntry map) throws IOException
	{
		int resultIndex;
		
		Profiler.beginTimingSection("addOwnerMap");
		mLock.writeLock().lock();
		
		try
		{
			mOwnerTagList.add(map);
			
			// Check if there is room in the file for this
			int hole = isRoomFor(OwnerMapEntry.cSize,mHeader.OwnerMapLocation + mHeader.OwnerMapSize);
			
			if(hole == -1 || (hole != mHoleIndex.size() && mHoleIndex.get(hole).AttachedTo != null))
			{
				// Prepare to relocate the ownermap
				HoleEntry oldIndexHole = new HoleEntry();
				oldIndexHole.Location = mHeader.OwnerMapLocation;
				oldIndexHole.Size = mHeader.OwnerMapSize;
				
				// Calculate the new header values
				mHeader.OwnerMapLocation = mFile.length();
				mHeader.OwnerMapCount = mOwnerTagList.size();
				mHeader.OwnerMapSize = mOwnerTagList.size() * OwnerMapEntry.cSize;
				
				// Append the index to the back of the file
				mFile.seek(mFile.length());
				for(int i = 0; i < mOwnerTagList.size(); i++)
					mOwnerTagList.get(i).write(mFile);

				Debug.fine("Owner tag Index relocated from %X -> (%X->%X)", oldIndexHole.Location, mHeader.OwnerMapLocation, mHeader.OwnerMapLocation + mHeader.OwnerMapSize-1);
				
				// Add the hole info
				addHole(oldIndexHole);
			}
			else
			{
				// Calculate the new header values
				mHeader.OwnerMapCount = mOwnerTagList.size();
				mHeader.OwnerMapSize = mOwnerTagList.size() * OwnerMapEntry.cSize;
				
				// Write it
				mFile.seek(mHeader.OwnerMapLocation + (mOwnerTagList.size()-1) * OwnerMapEntry.cSize);
				map.write(mFile);
				
				Debug.finer("Owner tag written at %X -> %X", mHeader.OwnerMapLocation + (mOwnerTagList.size()-1) * OwnerMapEntry.cSize, mHeader.OwnerMapLocation + mHeader.OwnerMapSize - 1);
				// Consume the hole
				if(hole != mHoleIndex.size())
					fillHole(hole,mHeader.OwnerMapLocation + (mOwnerTagList.size()-1) * OwnerMapEntry.cSize,OwnerMapEntry.cSize);
			}
			
			// Write the file header
			mFile.seek(0);
			mHeader.write(mFile);
			
			resultIndex = mOwnerTagList.size()-1;
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
		return resultIndex;
	}
	
	private void removeOwnerMap(int index) throws IOException
	{
		Debug.loggedAssert( index >= 0 && index < mOwnerTagList.size());
		
		Profiler.beginTimingSection("removeOwnerMap");
		mLock.writeLock().lock();
		
		
		try
		{
			// Shift the existing entries
			mFile.seek(mHeader.OwnerMapLocation + index * OwnerMapEntry.cSize);
			for(int i = index + 1; i < mOwnerTagList.size(); i++)
				mOwnerTagList.get(i).write(mFile);
			
			// Prepare the hole
			HoleEntry hole = new HoleEntry();
			hole.Location = mFile.getFilePointer();
			hole.Size = OwnerMapEntry.cSize;
			
			// Clear the last one
			mFile.write(new byte[OwnerMapEntry.cSize]);
			mOwnerTagList.remove(index);
			
			// Update the header
			mHeader.OwnerMapSize = mOwnerTagList.size() * OwnerMapEntry.cSize;
			mHeader.OwnerMapCount = mOwnerTagList.size();
			
			Debug.finer("Owner Map removed");
			// Write the file header
			mFile.seek(0);
			mHeader.write(mFile);
			
			addHole(hole);
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}
	
	private void addBlankRollbackEntry(IndexEntry session) throws IOException
	{
		Debug.loggedAssert(mHeader.VersionMajor >= 3, "Version 3 or higher is required to use the rollback index");
		
		RollbackEntry entry = new RollbackEntry();
		entry.sessionId = session.Id;
		entry.detailLocation = 0;
		entry.detailSize = 0;
		
		mRollbackEntries.add(entry);
		
		// Check if there is room in the file for this
		int hole = isRoomFor(RollbackEntry.cSize,mHeader.RollbackIndexLocation + mHeader.RollbackIndexSize);
		
		if(hole == -1 || (hole != mHoleIndex.size() && mHoleIndex.get(hole).AttachedTo != null))
		{
			// Prepare to relocate the index
			HoleEntry oldIndexHole = new HoleEntry();
			oldIndexHole.Location = mHeader.RollbackIndexLocation;
			oldIndexHole.Size = mHeader.RollbackIndexSize;
			
			// Calculate the new header values
			mHeader.RollbackIndexLocation = mFile.length();
			mHeader.RollbackIndexCount = mRollbackEntries.size();
			mHeader.RollbackIndexSize = mRollbackEntries.size() * RollbackEntry.cSize;
			
			// Append the index to the back of the file
			mFile.seek(mFile.length());
			for(int i = 0; i < mRollbackEntries.size(); i++)
				mRollbackEntries.get(i).write(mFile);
			
			Debug.finest("Rollback Index relocated from %X -> (%X->%X)", oldIndexHole.Location, mHeader.RollbackIndexLocation, mHeader.RollbackIndexLocation + mHeader.RollbackIndexSize-1);
			// Add the hole info
			addHole(oldIndexHole);
		}
		else
		{
			// Calculate the new header values
			mHeader.RollbackIndexCount = mRollbackEntries.size();
			mHeader.RollbackIndexSize = mRollbackEntries.size() * RollbackEntry.cSize;
			
			// Shift the index entries
			mFile.seek(mHeader.RollbackIndexLocation + mHeader.RollbackIndexSize);
			entry.write(mFile);
			
			Debug.finest("Writing 1 rollback index entries from %X -> %X", mHeader.RollbackIndexLocation + (mRollbackEntries.size() - 1) * RollbackEntry.cSize, mHeader.RollbackIndexLocation + mHeader.RollbackIndexSize - 1);
			// Consume the hole
			if(hole != mHoleIndex.size())
				fillHole(hole,mHeader.RollbackIndexLocation + (mRollbackEntries.size()-1) * RollbackEntry.cSize,RollbackEntry.cSize);
		}
		
		// Write the file header
		mFile.seek(0);
		
		if(mHeader.VersionMinor == 0)
			mHeader.VersionMinor = 1;
		
		mHeader.write(mFile);

		mRollbackMap.put(session.Id, mRollbackEntries.size()-1);
	}
	private void updateRollbackEntry(RollbackEntry entry) throws IOException
	{
		int index = mRollbackEntries.indexOf(entry);
		
		mFile.seek(mHeader.RollbackIndexLocation + index * RollbackEntry.cSize);
		entry.write(mFile);
		
		Debug.finest("Rollback entry @%d updated at %X -> %X", index, mHeader.RollbackIndexLocation + index * RollbackEntry.cSize, mHeader.RollbackIndexLocation + index * RollbackEntry.cSize + RollbackEntry.cSize - 1);
	}
	
	private short[] getRolledBackRecords(IndexEntry session) throws IOException
	{
		if(!mRollbackMap.containsKey(session.Id))
			return new short[0];
		
		RollbackListEntry list = getRollbackDetail(mRollbackEntries.get(mRollbackMap.get(session.Id)));
		if(list != null)
			return list.items;
		return new short[0];
	}
	
	private RollbackListEntry getRollbackDetail(RollbackEntry entry) throws IOException
	{
		if(entry.detailSize == 0)
			return null;
		
		mFile.seek(entry.detailLocation);
		
		RollbackListEntry list = new RollbackListEntry();
		list.read(mFile);
		
		return list;
	}
	
	private void updateDetail(RollbackEntry entry, RollbackListEntry detail, List<Short> newList) throws IOException
	{
		int diff = newList.size() - detail.items.length;
		if(diff < 0)
		{
			
		}
		else if(diff > 0)
		{
			int startIndex = 0;
			boolean isNew = entry.detailSize == 0;
			
			// Consume padding if there is any
			if(detail.padding >= 2 && !isNew)
			{
				int count = detail.padding / 2;
				count = Math.min(count, diff);
				
				detail.padding -= count * 2;
				short[] tempList = new short[detail.items.length + count];
				for(int i = 0; i < tempList.length; ++i)
					tempList[i] = newList.get(i);
				startIndex = tempList.length;
				
				detail.items = tempList;
				
				mFile.seek(entry.detailLocation);
				detail.write(mFile);
				
				Debug.finest("Rollback detail expanded by %d into padding. Location is unchanged: %X -> %X", count * 2, entry.detailLocation, entry.detailLocation + entry.detailSize - 1);
			}
			
			if(startIndex == newList.size())
				return;
			
			// If there is any left find somewhere to put it
			int hole = isRoomFor((newList.size()-startIndex)*2, entry.detailLocation + detail.getSize());
			
			// Copy the whole thing across
			short[] tempList = new short[newList.size()];
			for(int i = 0; i < tempList.length; ++i)
				tempList[i] = newList.get(i);
			detail.items = tempList;
			
			if(hole == -1 || (hole != mHoleIndex.size() && mHoleIndex.get(hole).AttachedTo != null))
			{
				// Place at end
				HoleEntry oldHole = new HoleEntry();
				oldHole.Location = entry.detailLocation;
				oldHole.Size = entry.detailSize;
				
				if(detail.padding < 8)
					detail.padding = 8;
				
				entry.detailLocation = mFile.length();
				entry.detailSize = detail.getSize();
				
				mFile.seek(entry.detailLocation);
				detail.write(mFile);
				
				// Update rollback entry
				updateRollbackEntry(entry);
				
				addHole(oldHole);
				
				Debug.finest("Rollback detail reloated to %X -> %X from %X -> %X", entry.detailLocation, entry.detailLocation + entry.detailSize - 1, oldHole.Location, oldHole.Location + oldHole.Size - 1);
			}
			else
			{
				// Consume hole
				mFile.seek(entry.detailLocation);
				detail.write(mFile);
				
				entry.detailSize = detail.getSize();
				updateRollbackEntry(entry);
				
				// Consume the hole
				if(hole != mHoleIndex.size())
				{
					Debug.finest("Rollback detail expanded into hole by %d. Location is now: %X -> %X", diff * 2, entry.detailLocation, entry.detailLocation + entry.detailSize - 1);
					fillHole(hole,entry.detailLocation + entry.detailSize - 2,2);
				}
				else
					Debug.finest("Rollback detail expanded by %d at eof. Location is now: %X -> %X", diff * 2, entry.detailLocation, entry.detailLocation + entry.detailSize - 1);
			}
		}
	}
	
	private void setRollbackStateInternal(IndexEntry session, List<Short> indices, boolean state) throws IOException
	{
		Debug.loggedAssert(session != null);
		
		Profiler.beginTimingSection("setRollbackState");
		mLock.writeLock().lock();
		
		Debug.info("Setting rollback state for %d records in session %d", indices.size(), session.Id);
		try
		{
			if(!mRollbackMap.containsKey(session.Id))
				addBlankRollbackEntry(session);
			
			RollbackEntry entry = mRollbackEntries.get(mRollbackMap.get(session.Id));

			// Get the existing detail
			RollbackListEntry list = getRollbackDetail(entry);
			
			if(list == null)
				list = new RollbackListEntry();

			// Modify the detail
			boolean[] add = new boolean[indices.size()];
			boolean[] remove = new boolean[indices.size()];
			
			for(int ind = 0; ind < indices.size(); ++ind)
			{
				add[ind] = true;
				remove[ind] = false;
				for(int i = 0; i < list.items.length; ++i)
				{
					if(list.items[i] == (short)indices.get(ind))
					{
						add[ind] = false;
						if(!state)
						{
							// Remove this item
							remove[ind] = true;
						}
						break;
					}
				}
			}
			
			ArrayList<Short> newList = new ArrayList<Short>(list.items.length);
			for(int i = 0; i < list.items.length; ++i)
				newList.add(list.items[i]);
			
			for(int i = 0; i < indices.size(); ++i)
			{
				if(remove[i])
					newList.remove(indices.get(i));
				if(add[i])
					newList.add(indices.get(i));
			}
			
			updateDetail(entry, list, newList);
		}
		finally
		{
			Debug.info("Completed setting rollback state");
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}
	public void setRollbackState(IndexEntry session, List<Short> indices, boolean state) throws IOException
	{
		mLock.writeLock().lock();
		
		try
		{
			mFile.beginTransaction();
			
			setRollbackStateInternal(session, indices, state);
			mFile.commit();
		}
		catch(Exception e)
		{
			mFile.rollback();
			throw e;
		}
		finally
		{
			mLock.writeLock().unlock();
		}
	}
	
	private void rebuildTagMap()
	{
		mLock.readLock().lock();
		
		
		mOwnerTagMap = new HashMap<Integer, Integer>();
		int index = 0;
		for(OwnerMapEntry tag : mOwnerTagList)
		{
			mOwnerTagMap.put(tag.Id, index);
			index++;
		}
		
		mLock.readLock().unlock();
		
	}
	private void rebuildIndexMap()
	{
		mLock.readLock().lock();
		
		
		mIndexMap = new HashMap<Integer, Integer>();
		int index = 0;
		for(IndexEntry session : mIndex)
		{
			mIndexMap.put(session.Id, index);
			index++;
		}
		
		mLock.readLock().unlock();
		
	}
	
	private void rebuildRollbackMap()
	{
		mLock.readLock().lock();
		
		
		mRollbackMap = new HashMap<Integer, Integer>();
		int index = 0;
		for(RollbackEntry entry : mRollbackEntries)
		{
			mRollbackMap.put(entry.sessionId, index);
			index++;
		}
		
		mLock.readLock().unlock();
	}
	private void readRollbackIndex(RandomAccessFile file, FileHeader header) throws IOException
	{
		Profiler.beginTimingSection("readRollbackIndex");
		mLock.writeLock().lock();
		
		try
		{
			mRollbackEntries = new ArrayList<RollbackEntry>();
		
			file.seek(header.RollbackIndexLocation);
			
			for(int i = 0; i < header.RollbackIndexCount; i++)
			{
				RollbackEntry entry = new RollbackEntry();
				entry.read(file);
				mRollbackEntries.add(entry);
			}
			
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}
	
	/**
	 *  Gets the name of the player whose activities are recorded here
	 */
	public String getName()
	{
		return mPlayerName;
	}
	/**
	 * Gets the file path of this log
	 */
	public File getFile()
	{
		return mFilePath;
	}

	private void readIndex(RandomAccessFile file, FileHeader header) throws IOException
	{
		Profiler.beginTimingSection("readIndex");
		mLock.writeLock().lock();
		
		
		try
		{
			mIndex = new ArrayList<IndexEntry>();
			
			// Read the index
			file.seek(header.IndexLocation);
		
			for(int i = 0; i < header.SessionCount; i++)
			{
				IndexEntry ent = new IndexEntry();
				ent.read(mHeader.VersionMajor,file);
				mIndex.add(ent);
			}
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}
	private void readHoles(RandomAccessFile file, FileHeader header) throws IOException
	{
		Profiler.beginTimingSection("readHoles");
		mLock.writeLock().lock();
		

		try
		{
			mHoleIndex = new ArrayList<HoleEntry>();
			
			// Read the index
			file.seek(header.HolesIndexLocation);
			
			for(int i = 0; i < header.HolesIndexCount; i++)
			{
				HoleEntry ent = new HoleEntry();
				ent.read(file);
				
				// Try to attach to a session
				for(IndexEntry session : mIndex)
				{
					if(!session.Compressed && ent.Location == session.Location + session.TotalSize)
					{
						ent.AttachedTo = session;
						break;
					}
				}
				
				mHoleIndex.add(ent);
			}

		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}
	private void readOwnerMap(RandomAccessFile file, FileHeader header) throws IOException
	{
		Profiler.beginTimingSection("readOwnerMap");
		mLock.writeLock().lock();
		
		
		try
		{
			mOwnerTagList = new ArrayList<OwnerMapEntry>();
		
			file.seek(header.OwnerMapLocation);
			
			for(int i = 0; i < header.OwnerMapCount; i++)
			{
				OwnerMapEntry entry = new OwnerMapEntry();
				entry.read(file);
				mOwnerTagList.add(entry);
			}
			
		}
		finally
		{
			mLock.writeLock().unlock();
			Profiler.endTimingSection();
		}
	}
	private int mReferenceCount = 0;
	
	private ReentrantReadWriteLock mLock;
	private ReentrantLock mReferenceLock;
	
	private boolean mIsLoaded = false;
	private boolean mIsCorrupt = false;
	private boolean mIsClosing = false;
	private int mTimeoutId = -1;
	private String mPlayerName;
	private ArrayList<IndexEntry> mIndex;
	private HashMap<Integer, Integer> mIndexMap;
	
	private ArrayList<HoleEntry> mHoleIndex;
	
	private ArrayList<OwnerMapEntry> mOwnerTagList;
	private HashMap<Integer, Integer> mOwnerTagMap;
	
	private ArrayList<RollbackEntry> mRollbackEntries;
	private HashMap<Integer, Integer> mRollbackMap;
	
	private ACIDRandomAccessFile mFile;
	
	private HashMap<String, Integer> mActiveSessions;
	private FileHeader mHeader;
	private File mFilePath;
	
	// This stuff is used to keep consistant state at all time
	private boolean mDeepMode;
	private Location mLastLocation;
	private InventoryRecord mLastInventory;
	
	/// The byte size that sessions should be cut down to. This may or may not be met depending on the size of the records. 
	public static long DesiredMaximumSessionSize = 102400;

	public static boolean sNoTimeoutOverride = false;
	
	// Task for closing the logfile when everything is executed
	private class CloseTask implements Task<Void> 
	{
		@Override
		public Void call() 
		{
			Debug.info("Closing log " + mPlayerName);
			
			try 
			{
				mFile.close();
			} 
			catch (IOException e) 
			{
				Debug.logException(e);
			}
			
			mIsLoaded = false;
			mIndex.clear();
			mIndex = null;
			mHoleIndex.clear();
			mHoleIndex = null;
			if(mOwnerTagList != null)
			{
				mOwnerTagList.clear();
				mOwnerTagList = null;
			}
			Debug.finest("CloseTask is completed");
			
			return null;
		}

		@Override
		public int getTaskTargetId()
		{
			return mPlayerName.hashCode();
		}

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

class OwnerMapEntry
{
	public static final int cMaxOwnerLength = 16;
	public static final int cSize = 4 + cMaxOwnerLength;
	
	public int Id;
	public String Owner;
	
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeInt(Id);
		byte[] ownerData = new byte[cMaxOwnerLength];
		Arrays.fill(ownerData, (byte)0);
		for(int i = 0; i < Owner.length() && i < cMaxOwnerLength; i++)
			ownerData[i] = (byte)Owner.charAt(i);
		
		file.write(ownerData);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		Id = file.readInt();
		char[] ownerData = new char[cMaxOwnerLength];
		for(int i = 0; i < cMaxOwnerLength; i++)
			ownerData[i] = (char)file.readByte();
		
		Owner = String.valueOf(ownerData);
		if(Owner.indexOf(0) != -1)
			Owner = Owner.substring(0, Owner.indexOf(0));
	}
}
