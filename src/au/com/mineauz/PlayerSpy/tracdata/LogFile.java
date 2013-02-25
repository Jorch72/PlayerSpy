package au.com.mineauz.PlayerSpy.tracdata;

import java.util.ArrayList;
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
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.LogTasks.*;
import au.com.mineauz.PlayerSpy.Records.*;
import au.com.mineauz.PlayerSpy.Utilities.ACIDRandomAccessFile;
import au.com.mineauz.PlayerSpy.Utilities.SafeChunk;
import au.com.mineauz.PlayerSpy.Utilities.Util;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
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
		if(mSessionIndex.getCount() > 0)
			result = mSessionIndex.get(0).StartTimestamp;
		mLock.readLock().unlock();
		
		return result;
	}
	
	public long getEndDate()
	{
		mLock.readLock().lock();
		
		long result = 0;
		if(mSessionIndex.getCount() > 0)
			result = mSessionIndex.get(mSessionIndex.getCount()-1).EndTimestamp;
		
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
		header.HolesIndexPadding = 0;
		
		header.IndexLocation = header.getSize();
		header.IndexSize = 0;
		header.SessionCount = 0;
		
		header.OwnerMapCount = 0;
		header.OwnerMapSize = 0;
		header.OwnerMapLocation = header.getSize();
		
		header.RollbackIndexCount = 0;
		header.RollbackIndexSize = 0;
		header.RollbackIndexLocation = header.getSize();
		
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
		
		log.mSpaceLocator = new SpaceLocator();
		log.mHoleIndex = new HoleIndex(log, header, file, log.mSpaceLocator);
		log.mSpaceLocator.setHoleIndex(log.mHoleIndex);
		
		log.mSessionIndex = new SessionIndex(log, header, file, log.mSpaceLocator);
		log.mOwnerTagIndex = new OwnerTagIndex(log, header, file, log.mSpaceLocator);
		log.mRollbackIndex = new RollbackIndex(log, header, file, log.mSpaceLocator);
		
		
		log.mIsLoaded = true;
		log.mFile = file;
		log.mHeader = header;
		log.mReferenceCount = 1;
		
		
		Debug.fine("Created a log file for '" + playerName + "'.");
		
		CrossReferenceIndex.instance.addLogFile(log);
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
			
			// Initialize the indexes
			mSpaceLocator = new SpaceLocator();
			mHoleIndex = new HoleIndex(this, mHeader, file, mSpaceLocator);
			mSpaceLocator.setHoleIndex(mHoleIndex);
			
			mSessionIndex = new SessionIndex(this, mHeader, file, mSpaceLocator);
			mOwnerTagIndex = new OwnerTagIndex(this, mHeader, file, mSpaceLocator);
			mRollbackIndex = new RollbackIndex(this, mHeader, file, mSpaceLocator);
			
			// Read the indices
			mHoleIndex.read();
			mSessionIndex.read();
			
			mHoleIndex.applyReservations(mSessionIndex);
			
			if(header.VersionMajor >= 2)
				mOwnerTagIndex.read();
			
			if(header.VersionMajor >= 3 && header.VersionMinor >= 1)
				mRollbackIndex.read();

			
			mPlayerName = header.PlayerName;
			
			mFile = file;
			
			Debug.info("Load Succeeded:");
			Debug.info(" Player: " + mPlayerName);
			Debug.fine(" Sessions found: " + mHeader.SessionCount);
			Debug.fine(" Holes found: " + mHeader.HolesIndexCount);
			if(mSessionIndex.getCount() > 0)
			{
				Debug.fine(" Earliest Date: " + Util.dateToString(mSessionIndex.get(0).StartTimestamp));
				Debug.fine(" Latest Date: " + Util.dateToString(mSessionIndex.get(mSessionIndex.getCount()-1).EndTimestamp));
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
	public List<SessionEntry> getSessions()
	{
		return mSessionIndex.getEntries();
	}
	public SessionEntry getSessionById(int id)
	{
		return mSessionIndex.getSessionFromId(id);
	}
	public String getOwnerTag(SessionEntry session)
	{
		if(session.OwnerTagId == -1)
			return null;
		
		mLock.readLock().lock();
		
		OwnerMapEntry entry = mOwnerTagIndex.getOwnerTagById(session.OwnerTagId);
		
		String result = null;
		if(entry != null)
			result = entry.Owner;
				
		mLock.readLock().unlock();
		
		return result;
	}
	public String getOwnerTag(int sessionIndex)
	{
		return getOwnerTag(mSessionIndex.get(sessionIndex));
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
		
		for(SessionEntry entry : mSessionIndex)
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
		
		
		for(int i = mSessionIndex.getCount()-1; i >=0 ; --i)
		{
			SessionEntry entry = mSessionIndex.get(i);
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
		ArrayList<SessionEntry> relevantEntries = new ArrayList<SessionEntry>();
		
		Debug.fine("Loading records from " + Util.dateToString(startDate) + " to " + Util.dateToString(endDate));
		
		// We will hold the write lock because accessing the file concurrently through the same object with have issues i think.
		mLock.writeLock().lock();
		
				
		// Find the relevant sessions
		for(SessionEntry entry : mSessionIndex)
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
		for(SessionEntry session : relevantEntries)
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
		ArrayList<SessionEntry> relevantEntries = new ArrayList<SessionEntry>();
		
		// We will hold the write lock because accessing the file concurrently through the same object with have issues i think.
		mLock.writeLock().lock();
					
		
		// Find the relevant sessions
		for(SessionEntry entry : mSessionIndex)
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
		for(SessionEntry session : relevantEntries)
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
	public RecordList loadSession(SessionEntry session)
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
					Debug.warning("Corruption in " + mPlayerName + ".tracdata session " + session.Id + " found. Attempting to fix");
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
					Debug.warning("Issue detected with " + mPlayerName + ".trackdata in session " + session.Id + ". No world has been set. Defaulting to main world");
					lastWorld = Bukkit.getWorlds().get(0);
					records.add(new WorldChangeRecord(lastWorld));
				}
				if(!hadInv && i > 3 && !isAbsolute)
				{
					Debug.warning("Issue detected with " + mPlayerName + ".trackdata in session " + session.Id + ". No inventory state has been set. ");
					hadInv = true;
				}
				records.add(record);
			}
			
			// Load the rollback state info in
			short[] indices = getRolledBackRecords(session);
			for(int i = 0; i < indices.length; ++i)
			{
				if(indices[i] < 0 || indices[i] >= records.size())
					continue;
				
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
	private RecordList appendRecords(RecordList records, SessionEntry session) throws IOException
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
			HoleEntry hole = mHoleIndex.getHoleAfter(session.Location + session.TotalSize);
			if(hole != null)
			{
				if(hole.AttachedTo != null && hole.AttachedTo != session)
					hole = null;
				else
					availableSpace = hole.Size;
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
				mSpaceLocator.consumeSpace(session.Location + session.TotalSize,bstream.size(), session);
				
				// Update the session info
				session.TotalSize += bstream.size();
				session.EndTimestamp = records.getEndTimestamp();
				session.RecordCount += records.size();
				mSessionIndex.set(mSessionIndex.indexOf(session), session);
				
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
					
					mFile.seek(session.Location);
					mFile.write(ostream.toByteArray());
					
					mSpaceLocator.releaseSpace(session.Location + ostream.size(), session.TotalSize - ostream.size());
					
					session.TotalSize = ostream.size();
					session.Compressed = true;
					
					mSessionIndex.set(mSessionIndex.indexOf(session), session);

					if(hole != null)
						hole.AttachedTo = null;
					
					pullData(session.Location + ostream.size());
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

		boolean result;
		
		Profiler.beginTimingSection("appendRecords");
		synchronized (CrossReferenceIndex.instance)
		{
			mLock.writeLock().lock();
			
			try
			{
				mFile.beginTransaction();
				
				Debug.info("Appending " + records.size() + " records to " + mPlayerName + ">" + owner);
				
				SessionEntry activeSession = mSessionIndex.getActiveSessionFor(owner);
				if(activeSession == null)
				{
					Debug.info("No active session for %s>%s. Creating one", mPlayerName, owner);
					
					int index = initialiseSession(records, true);
					if(index != -1)
					{
						
						mSessionIndex.setActiveSession(owner, mSessionIndex.get(index));
	
						SessionEntry session = mSessionIndex.get(index);
						session.OwnerTagId = -1;
						
						// See if there is a tag we can reuse
						for(OwnerMapEntry tag : mOwnerTagIndex)
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
							
							mOwnerTagIndex.add(ent);
						}
	
						mSessionIndex.set(index, session);
						
						result = true;
					}
					else
						result = false;
				}
				else
				{
					
					RecordList splitSession = appendRecords(records, activeSession);
		
					if(splitSession != null && splitSession.size() > 0)
					{
						int index = initialiseSession(splitSession, true);
						if(index != -1)
						{
							mSessionIndex.setActiveSession(owner, mSessionIndex.get(index));
		
							SessionEntry session = mSessionIndex.get(index);
							session.OwnerTagId = -1;
							
							// See if there is a tag we can reuse
							for(OwnerMapEntry tag : mOwnerTagIndex)
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
								
								mOwnerTagIndex.add(ent);
							}
		
							mSessionIndex.set(index, session);
							
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
				
				SessionEntry activeSession = mSessionIndex.getActiveSessionFor(null);
				
				if(activeSession == null)
				{
					Debug.finer("Tried to append records. No active session was found.");
					int index = initialiseSession(records, false);
					if(index != -1)
					{
						mSessionIndex.setActiveSession(null, mSessionIndex.get(index));
						result = true;
					}
					else
						result = false;
				}
				else
				{
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
							mSessionIndex.setActiveSession(null, mSessionIndex.get(index));
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
			
			SessionEntry session = new SessionEntry();
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
			
			session.Location = mSpaceLocator.findFreeSpace(Math.max(session.TotalSize,DesiredMaximumSessionSize));
		
			// Write the session
			if(session.Location == mHoleIndex.getEndOfFile())
			{
				// Append to the file
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
					
					mHoleIndex.add(hole);
				}
			}
			else
			{
				// Write it in the hole
				mFile.seek(session.Location);
				Debug.finest(" Writing session %d to %X -> %X", session.Id, session.Location, session.Location + bstream.size() - 1);
				mFile.write(bstream.toByteArray());
			}
			
			mSpaceLocator.consumeSpace(session.Location, session.TotalSize);
			
			// Write the index entry
			int id = mSessionIndex.add(session);
			
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
				ArrayList<SessionEntry> relevantEntries = new ArrayList<SessionEntry>();
				
				// Find the relevant sessions
				for(SessionEntry entry : mSessionIndex)
				{
					if((fromDate >= entry.StartTimestamp && fromDate <= entry.EndTimestamp) ||
					   (toDate >= entry.StartTimestamp && toDate <= entry.EndTimestamp) ||
					   (entry.StartTimestamp >= fromDate && entry.StartTimestamp < toDate) ||
					   (entry.EndTimestamp > fromDate && entry.EndTimestamp < toDate))
						relevantEntries.add(entry);
				}
				Debug.finer("  " + relevantEntries.size() + " Matching Sessions");
				
				// Purge data
				for(SessionEntry entry : relevantEntries)
				{
					String otag = getOwnerTag(entry);
					boolean isAbsolute = otag != null;
					if(entry.StartTimestamp >= fromDate && entry.EndTimestamp < toDate)
					{
						// Whole session must be purged
						int index = mSessionIndex.indexOf(entry);
						if(index == -1)
							continue;
						
						mSessionIndex.remove(index);
						
						if(mSessionIndex.getActiveSessionFor(otag) != null && mSessionIndex.getActiveSessionFor(otag).Id == entry.Id)
							mSessionIndex.setActiveSession(otag, null);
	
						// Pull the proceeding data forward
						pullData(entry.Location);
						
						// Purge the owner tag if no session uses it
						int count = 0;
						for(SessionEntry session : mSessionIndex)
						{
							if(session.Id == entry.Id)
								continue;
							if(session.OwnerTagId == entry.OwnerTagId)
								count++;
						}
						
						if(count == 0)
							mOwnerTagIndex.remove(mOwnerTagIndex.getOwnerTagById(entry.OwnerTagId));
						
						// So that anything else using this object through a reference, wont do any damage
						entry.RecordCount = 0;
						entry.Location = 0;
						entry.Id = -1;
						entry.TotalSize = 0;
						entry.OwnerTagId = -1;
					}
					else
					{
						int sessionIndex = mSessionIndex.indexOf(entry);
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
							mSessionIndex.remove(sessionIndex);
							
							if(mSessionIndex.getActiveSessionFor(otag) != null && mSessionIndex.getActiveSessionFor(otag).Id == entry.Id)
								mSessionIndex.setActiveSession(otag, null);
							
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
							
							
							long oldSize = entry.TotalSize;
							
							// Write to file
							mFile.seek(entry.Location);
							mFile.write(bstream.toByteArray());
							
							// Update the session header
							entry.Compressed = false;
							entry.StartTimestamp = sessionData.getStartTimestamp();
							entry.EndTimestamp = sessionData.getEndTimestamp();
							entry.RecordCount = (short) sessionData.size();
							entry.TotalSize = totalSize;
							
							mSessionIndex.set(sessionIndex,entry);
							CrossReferenceIndex.instance.updateSession(this, entry, new ArrayList<SafeChunk>());
							
							mSpaceLocator.releaseSpace(entry.Location + entry.TotalSize, oldSize - entry.TotalSize);
							
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
		HoleEntry holeData = mHoleIndex.getHoleAfter(location);
		// TODO: Remove the recursion here
		if(holeData != null)
		{
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
			
			for(SessionEntry nextSession : mSessionIndex)
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
					nextSize = mHeader.HolesIndexSize;
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
					for(RollbackEntry nextEntry : mRollbackIndex)
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
				Utility.shiftBytes(mFile, nextLocation, holeData.Location, nextSize);
				
				HoleEntry old = new HoleEntry();
				old.Location = holeData.Location + nextSize;
				old.Size = holeData.Size;
				
				// Update whatever
				switch(type)
				{
				case 0: // session
				{
					SessionEntry nextSession = mSessionIndex.get(index);
					Debug.finest("Shifted session %d from %X -> (%X-%X)", nextSession.Id, nextSession.Location, holeData.Location, holeData.Location + nextSize - 1);
					nextSession.Location = holeData.Location;
					mSessionIndex.set(index, nextSession);
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
					RollbackEntry nextEntry = mRollbackIndex.get(index);
					Debug.finest("Shifted rollback detail for %d from %X -> (%X-%X)", nextEntry.sessionId, nextEntry.detailLocation, holeData.Location, holeData.Location + nextSize - 1);
					nextEntry.detailLocation = holeData.Location;
					mRollbackIndex.set(index, nextEntry);
					break;
				}
				}
				
				// Move the hole
				mHoleIndex.remove(holeData);
				
				// Add in the new hole
				mHoleIndex.add(old);
				
				// Attempt to compact further stuff
				pullData(old.Location);
			}
			else
			{
				// Nothing to pull because there is no more data after us
				mHoleIndex.remove(holeData);
				// Trim the file
				mFile.setLength(holeData.Location);
			}
		}
		Profiler.endTimingSection();
	}
	
	private short[] getRolledBackRecords(SessionEntry session) throws IOException
	{
		if(mRollbackIndex.getRollbackEntryById(session.Id) == null)
			return new short[0];
		
		RollbackListEntry list = getRollbackDetail(mRollbackIndex.getRollbackEntryById(session.Id));
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
			
			long oldSize = detail.getSize();
			
			// Copy the whole thing across
			short[] tempList = new short[newList.size()];
			for(int i = 0; i < tempList.length; ++i)
				tempList[i] = newList.get(i);
			detail.items = tempList;
			
			if(mSpaceLocator.isFreeSpace(entry.detailLocation + oldSize, detail.getSize() - oldSize))
			{
				mFile.seek(entry.detailLocation);
				detail.write(mFile);
				
				entry.detailSize = detail.getSize();
				mRollbackIndex.set(mRollbackIndex.indexOf(entry), entry);
				
				// Consume the hole
				mSpaceLocator.consumeSpace(entry.detailLocation + oldSize, detail.getSize() - oldSize);
				Debug.finest("Rollback detail expanded by %d. Location is now: %X -> %X", diff * 2, entry.detailLocation, entry.detailLocation + entry.detailSize - 1);
			}
			else
			{
				long oldLocation = entry.detailLocation;
				oldSize = entry.detailSize;
				
				if(detail.padding < 8)
					detail.padding = 8;
				
				entry.detailSize = detail.getSize();
				entry.detailLocation = mSpaceLocator.findFreeSpace(entry.detailSize);
				
				mFile.seek(entry.detailLocation);
				detail.write(mFile);
				
				// Update rollback entry
				mRollbackIndex.set(mRollbackIndex.indexOf(entry), entry);
				
				mSpaceLocator.consumeSpace(entry.detailLocation, entry.detailSize);
				mSpaceLocator.releaseSpace(oldLocation, oldSize);
				
				Debug.finest("Rollback detail reloated to %X -> %X from %X -> %X", entry.detailLocation, entry.detailLocation + entry.detailSize - 1, oldLocation, oldLocation + oldSize - 1);
			}
		}
	}
	
	private void setRollbackStateInternal(SessionEntry session, List<Short> indices, boolean state) throws IOException
	{
		Debug.loggedAssert(session != null);
		
		Profiler.beginTimingSection("setRollbackState");
		mLock.writeLock().lock();
		
		Debug.info("Setting rollback state for %d records in session %d", indices.size(), session.Id);
		try
		{
			RollbackEntry entry = mRollbackIndex.getRollbackEntryById(session.Id);
			
			if(entry == null)
			{
				entry = new RollbackEntry();
				entry.sessionId = session.Id;
				mRollbackIndex.add(entry);
			}

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
	public void setRollbackState(SessionEntry session, List<Short> indices, boolean state) throws IOException
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

	private int mReferenceCount = 0;
	
	ReentrantReadWriteLock mLock;
	private ReentrantLock mReferenceLock;
	
	private boolean mIsLoaded = false;
	private boolean mIsCorrupt = false;
	private boolean mIsClosing = false;
	private int mTimeoutId = -1;
	private String mPlayerName;

	SessionIndex mSessionIndex;
	HoleIndex mHoleIndex;
	OwnerTagIndex mOwnerTagIndex;
	RollbackIndex mRollbackIndex;
	
	private SpaceLocator mSpaceLocator;
	
	private ACIDRandomAccessFile mFile;
	
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
