package au.com.mineauz.PlayerSpy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPInputStream;
import java.io.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import au.com.mineauz.PlayerSpy.LogTasks.*;
import au.com.mineauz.PlayerSpy.Records.*;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;

public class LogFile 
{
	private static final ExecutorService mAsyncService = Executors.newSingleThreadExecutor(); 
	private static int NextId = (int)(System.currentTimeMillis() / 1000);
	
	private ReentrantReadWriteLock mLock;
	private ReentrantLock mReferenceLock;
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
	@SuppressWarnings("resource")
	public static LogFile create(String playerName, String filename)
	{
		RandomAccessFile file = null;
		try 
		{
			file = new RandomAccessFile(filename, "rw");
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
		
		header.RequiresOwnerTags = false;
		
		if(!header.write(file))
		{
			try 
			{
				file.close();
			} 
			catch (IOException e) {}
			
			return null;
		}
		
		try 
		{
			// write some padding in
			file.write(new byte[HoleEntry.cSize]);
		} 
		catch (IOException e) {}
		
		// initialize the logfile instance
		
		LogFile log = new LogFile();
		log.mFilePath = new File(filename);
		log.mPlayerName = playerName;
		log.mIndex = new ArrayList<IndexEntry>();
		log.mHoleIndex = new ArrayList<HoleEntry>();
		log.mOwnerTagList = new ArrayList<OwnerMapEntry>();
		log.mActiveSessions = new HashMap<String, Integer>();
		log.mIsLoaded = true;
		log.mFile = file;
		log.mHeader = header;
		log.mReferenceCount = 1;
		
		log.rebuildTagMap();
		log.rebuildIndexMap();
		
		LogUtil.fine("Created a log file for '" + playerName + "'.");
		
		CrossReferenceIndex.instance.addLogFile(log);
		return log;
	}
	/**
	 * Creates a blank log file for world global entries ready to accept sessions.
	 * It will be open when returned so remember to call close() when you're done
	 * @param the name of the world to use including the global prefix
	 * @param filename The filename to create the log at
	 * @return An instance with an open log or null if unable to create it
	 */
	public static LogFile createGlobal(String worldName, String filename)
	{
		LogFile log = create(worldName, filename);
		
		log.mHeader.RequiresOwnerTags = true;

		try
		{
			log.mFile.seek(0);
			log.mHeader.write(log.mFile);
		}
		catch(IOException e)
		{
			
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
			if(!header.read(file))
				header = null;
			
			file.close();
			
			return header;
		} catch (Exception e) 
		{
			e.printStackTrace();
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
		boolean ok = false;
		mLock.writeLock().lock();
		
		try
		{
			LogUtil.fine("Loading '" + filename + "'...");
			mFilePath = new File(filename);
			RandomAccessFile file = new RandomAccessFile(filename, "rw");
			
			// Read the file header
			FileHeader header = new FileHeader();
			if(!header.read(file ))
			{
				SpyPlugin.getInstance().getLogger().severe("Failed to load log file. Reason: Header incorrect.");
				mLock.writeLock().unlock();
				return false;
			}
			mHeader = header;
			
			// Read the index
			readIndex(file,header);
			
			// Read the holes index
			readHoles(file,header);
			
			if(header.VersionMajor >= 2)
				readOwnerMap(file, header);
			
			rebuildTagMap();
			rebuildIndexMap();
			
			mPlayerName = header.PlayerName;
			
			mFile = file;
			
			mActiveSessions = new HashMap<String, Integer>();
			if(mIndex.size() > 0)
			{
				for(int i = mIndex.size() - 1; i >= 0; --i)
				{
					
					if(!mActiveSessions.containsKey(getOwnerTag(i)))
					{
						mActiveSessions.put(getOwnerTag(i), mIndex.get(i).Id);
						break;
					}
				}
			}
			
			LogUtil.fine("Load Succeeded:");
			LogUtil.fine(" Player: " + mPlayerName);
			LogUtil.fine(" Sessions found: " + mHeader.SessionCount);
			LogUtil.fine(" Holes found: " + mHeader.HolesIndexCount);
			if(mIndex.size() > 0)
			{
				LogUtil.fine(" Earliest Date: " + Util.dateToString(mIndex.get(0).StartTimestamp));
				LogUtil.fine(" Latest Date: " + Util.dateToString(mIndex.get(mIndex.size()-1).EndTimestamp));
			}
			
			mIsLoaded = true;
			ok = true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			ok = false;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ok = false;
		}
		
		mLock.writeLock().unlock();
		return ok;
	}

	/**
	 * Decreases the reference count and closes the log file if needed
	 */
	public void close(boolean noTimeout)
	{
		assert mIsLoaded && !mIsClosing && mTimeoutId == -1;
		
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
					Future<?> future = mAsyncService.submit(new CloseTask());
					future.get();
				} 
				catch (InterruptedException e1) 
				{
					e1.printStackTrace();
				} 
				catch (ExecutionException e) 
				{
					e.printStackTrace();
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
						LogUtil.finer("Submitting the close task");
						mAsyncService.submit(new CloseTask());
					}
				}, SpyPlugin.getSettings().logTimeout / 50L);
			}
		}
		else
			mReferenceLock.unlock();
	}
	/**
	 * Decreases the reference count and closes the log file if needed
	 * This method is Asynchronous
	 */
	public void closeAsync(boolean noTimeout)
	{
		assert mIsLoaded && !mIsClosing && mTimeoutId == -1;
		
		mReferenceLock.lock();
		mReferenceCount--;
		
		if(mReferenceCount <= 0)
		{
			mReferenceLock.unlock();
			if(noTimeout || sNoTimeoutOverride)
			{
				mIsClosing = true;
				LogUtil.finer("Submitting the close task");
				mAsyncService.submit(new CloseTask());
			}
			else
			{
				mTimeoutId = Bukkit.getScheduler().scheduleSyncDelayedTask(SpyPlugin.getInstance(), new Runnable() 
				{
					@Override
					public void run() 
					{
						mIsClosing = true;
						LogUtil.finer("Submitting the close task");
						mAsyncService.submit(new CloseTask());
					}
				}, SpyPlugin.getSettings().logTimeout / 50L);
			}
		}
		else
			mReferenceLock.unlock();
	}
	
	public Future<Long> submitTask(Callable<Long> task)
	{
		return mAsyncService.submit(task);
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
		assert mIsLoaded;
		assert mHeader.VersionMajor >= 2 : "Owner tags are only suppored in version 2 and above";
		if(!mHeader.RequiresOwnerTags)
			throw new IllegalStateException("Owner tags are not enabled in this log");
		
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

		LogUtil.fine("  " + allRecords.size() + " returned records");
		
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
		LogUtil.finer("Submitting loadRecords async task");
		synchronized (mAsyncService)
		{
			return mAsyncService.submit(new LoadRecordsAsyncTask(this, startDate, endDate, false, owner));
		}
	}
	/**
	 * Loads records between the specified dates and returns them
	 * @param startDate The earliest date to retrieve records for
	 * @param endDate The latest date to retrieve records for
	 * @return The list of records whose timestamps fall between the start and end dates 
	 */
	public RecordList loadRecords(long startDate, long endDate)
	{
		assert mIsLoaded;
		
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

		LogUtil.fine("  " + allRecords.size() + " returned records");
		
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
		synchronized (mAsyncService)
		{
			LogUtil.finer("Submitting loadRecords async task");
			return mAsyncService.submit(new LoadRecordsAsyncTask(this, startDate, endDate, false));
		}
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
		assert mIsLoaded;
		assert mHeader.VersionMajor >= 2 : "Owner tags are only suppored in version 2 and above";
		if(!mHeader.RequiresOwnerTags)
			throw new IllegalStateException("Owner tags are not enabled in this log");
		
		ArrayList<IndexEntry> relevantEntries = new ArrayList<IndexEntry>();
		
		LogUtil.fine("Loading records from " + Util.dateToString(startDate) + " to " + Util.dateToString(endDate));
		
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
		LogUtil.finer("  " + relevantEntries.size() + " Matching Sessions");
		// Now load up the records
		RecordList allRecords = new RecordList();
		for(IndexEntry session : relevantEntries)
		{
			allRecords.addAll(loadSession(session));
		}
		
		mLock.writeLock().unlock();
		
		LogUtil.finer("  " + allRecords.size() + " loaded records");

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
		synchronized (mAsyncService)
		{
			LogUtil.finer("Submitting loadRecordChunks async task");
			return mAsyncService.submit(new LoadRecordsAsyncTask(this, startDate, endDate, true, owner));
		}
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
		assert mIsLoaded;
		
		LogUtil.fine("Loading chunks from " + Util.dateToString(startDate) + " to " + Util.dateToString(endDate));
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
		LogUtil.finer("  " + relevantEntries.size() + " Matching Sessions");
		// Now load up the records
		RecordList allRecords = new RecordList();
		for(IndexEntry session : relevantEntries)
		{
			allRecords.addAll(loadSession(session));
		}
		LogUtil.fine("  " + allRecords.size() + " loaded records");
		
		mLock.writeLock().unlock();
		
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
		synchronized (mAsyncService)
		{
			LogUtil.finer("Submitting loadRecordChunks async task");
			return mAsyncService.submit(new LoadRecordsAsyncTask(this, startDate, endDate, true));
		}
	}
	public RecordList loadSession(IndexEntry session)
	{
		assert mIsLoaded;
		
		RecordList records = null;
		
		boolean isAbsolute = getOwnerTag(session) != null;
		
		// We will hold the write lock because accessing the file concurrently through the same object with have issues i think.
		mLock.writeLock().lock();
						
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
					LogUtil.severe("Read NULL record");
					break;
				}
				
				// update the last world
				if(record instanceof ILocationAware && ((ILocationAware)record).isFullLocation())
					lastWorld = ((ILocationAware)record).getLocation().getWorld();
				else if(record instanceof WorldChangeRecord)
					lastWorld = ((WorldChangeRecord)record).getWorld();
				else if((lastWorld == null && record.getType() != RecordType.FullInventory && record.getType() != RecordType.EndOfSession) && !isAbsolute)
				{
					LogUtil.warning("Corruption in " + mPlayerName + ".tracdata session " +mIndex.indexOf(session) + " found. Attempting to fix");
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
					LogUtil.warning("Issue detected with " + mPlayerName + ".trackdata in session " + mIndex.indexOf(session) + ". No world has been set. Defaulting to main world");
					lastWorld = Bukkit.getWorlds().get(0);
					records.add(new WorldChangeRecord(lastWorld));
				}
				if(!hadInv && i > 3 && !isAbsolute)
				{
					LogUtil.warning("Issue detected with " + mPlayerName + ".trackdata in session " + mIndex.indexOf(session) + ". No inventory state has been set. ");
					hadInv = true;
				}
				records.add(record);
			}
			
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		mLock.writeLock().unlock();
		
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
		LogUtil.fine("Appending " + records.size() + " records:");
		
		// This will be populated if there are too many records to put into this session
		RecordList splitSession = null;
		boolean isAbsolute = getOwnerTag(session) != null;
		
		mLock.writeLock().lock();
		
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
			
			LogUtil.finer(" Avaiable Space: " + availableSpace);
			
			// Calculate the size of the records
			long totalSize = 0;
			int cutoffIndex = 0;
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
			}
			
			LogUtil.finer(" Total size to write: " + totalSize);
			if(splitSession != null)
				LogUtil.finer(" Cutoff at: " + cutoffIndex);
			
			if(records.size() != 0)
			{
				// Encode the records
				ByteArrayOutputStream bstream = new ByteArrayOutputStream((int)totalSize);
				DataOutputStream dstream = new DataOutputStream(bstream);
				long lastSize = dstream.size();
				for(int i = 0; i < cutoffIndex; i++)
				{
					int expectedSize = records.get(i).getSize(isAbsolute);
					records.get(i).write(dstream, isAbsolute);
					
					long actualSize = dstream.size() - lastSize;
					
					if(expectedSize != actualSize)
					{
						LogUtil.severe(records.get(i).getType().toString() + " is returning incorrect size. Expected: " + expectedSize + " got " + actualSize);
					}
					lastSize = dstream.size();
				}
		
				// ensure i havent messed up the implementation of getSize()
				assert totalSize == bstream.size();
				
				// Write it into the file
				mFile.seek(session.Location + session.TotalSize);
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
			}
		}
		finally
		{
			mLock.writeLock().unlock();
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
		assert mIsLoaded;
		assert mHeader.VersionMajor >= 2 : "Owner tags are only suppored in version 2 and above";
		if(mOwnerTagList == null)
		{
			LogUtil.severe("OwnerMap is null. Log: " + getName() + " Version: " + mHeader.VersionMajor + "." + mHeader.VersionMinor);
			return false;
		}

		mLock.writeLock().lock();
		
		boolean result;
		
		try
		{
			LogUtil.fine("Appending " + records.size() + " records to " + mPlayerName + ">" + owner);
			
			if(!mActiveSessions.containsKey(owner) || getSessionById(mActiveSessions.get(owner)) == null)
			{
				int index = initialiseSession(records, true);
				if(index != -1)
				{
					mActiveSessions.put(owner, mIndex.get(index).Id);

					IndexEntry session = mIndex.get(index);
					session.OwnerTagId = NextId++;
					
					// Add the tag
					OwnerMapEntry ent = new OwnerMapEntry();
					ent.Owner = owner;
					ent.Id = session.OwnerTagId;

					updateSession(index,session);
					mOwnerTagMap.put(session.OwnerTagId, addOwnerMap(ent));
					
					result = true;
				}
				else
					result = false;
			}
			else
			{
				
				IndexEntry activeSession = getSessionById(mActiveSessions.get(owner));
				
				RecordList splitSession = appendRecords(records, activeSession);
	
				if(splitSession != null)
				{
					int index = initialiseSession(splitSession, true);
					if(index != -1)
					{
						mActiveSessions.put(owner, mIndex.get(index).Id);
	
						IndexEntry session = mIndex.get(index);
						session.OwnerTagId = NextId++;
						
						// Add the tag
						OwnerMapEntry ent = new OwnerMapEntry();
						ent.Owner = owner;
						ent.Id = session.OwnerTagId;
	
						updateSession(index,session);
						mOwnerTagMap.put(session.OwnerTagId, addOwnerMap(ent));
						
						result = true;
					}
					else
						result = false;
				}
				else
					result = true;
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			result = false;
		}
		mLock.writeLock().unlock();
		return result;
	}
	/**
	 * Appends records onto the active session.
	 * If there is no active session, a new session will be created
	 * @param records The list of records to append
	 * @return True if append was successful
	 * @throws IllegalStateException if owner tags are required in the log file. You must use the other version of appendRecords()
	 */
	public synchronized boolean appendRecords(RecordList records)
	{
		assert mIsLoaded;
		if(mHeader.RequiresOwnerTags && mHeader.VersionMajor >= 2)
			throw new IllegalStateException("Owner tags are required. You can only append records through appendRecords(records, tag)");
		
		LogUtil.fine("Appending " + records.size() + " records to " + mPlayerName);
		boolean result;
		mLock.writeLock().lock();
		
		try
		{
			if(!mActiveSessions.containsKey(null) || getSessionById(mActiveSessions.get(null)) == null)
			{
				LogUtil.finer("Tried to append records. No active session was found.");
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
				
				// Make sure the remaining records are written
				if(splitSession != null)
				{
					if(!splitSession.isIndependant() && mDeepMode)
					{
						LogUtil.finest("Record List is dependant. Now searching through active session to find latest inventory and position info");
						// Load back some data 
						mFile.seek(activeSession.Location);
						byte[] data = new byte[(int) activeSession.TotalSize];
						mFile.read(data);
						
						ByteArrayInputStream istream = new ByteArrayInputStream(data);
						DataInputStream stream = new DataInputStream(istream);
						
						World currentWorld = null;
						Location currentLocation = null;
						InventoryRecord currentInventory = null;
						
						// Find the latest position, and inventory
						for(int i = 0; i < activeSession.RecordCount; i++)
						{
							Record record = Record.readRecord(stream, currentWorld, mHeader.VersionMajor, false);
							
							if(record instanceof ILocationAware)
							{
								currentLocation = ((ILocationAware)record).getLocation();
								if(((ILocationAware)record).isFullLocation())
									currentWorld = currentLocation.getWorld();
							}
							switch(record.getType())
							{
							case FullInventory:
								currentInventory = (InventoryRecord) record;
								break;
							case WorldChange:
								currentWorld = ((WorldChangeRecord)record).getWorld();
								break;
							case UpdateInventory:
								if(currentInventory != null)
								{
									UpdateInventoryRecord urecord = (UpdateInventoryRecord)record;
									
									for(InventorySlot slot : urecord.Slots)
									{
										currentInventory.getItems()[slot.Slot] = (slot.Item == null ? null : slot.Item.clone());
									}
								}
								break;
							default:
								break;
							}
						}
						
						if(currentInventory == null)
							LogUtil.warning("Bug detected. Finalising session and no inventory has been set at all");
						else
							splitSession.add(0,currentInventory);
						
						if(currentLocation == null)
							LogUtil.warning("Bug detected. Finalising session and no position has been set at all");
						else
							splitSession.add(0, new TeleportRecord(currentLocation, TeleportCause.UNKNOWN));
					}
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
		}
		catch(IOException e)
		{
			e.printStackTrace();
			result = false;
		}
		
		mLock.writeLock().unlock();
		return result;
	}
	
	public Future<Boolean> appendRecordsAsync(RecordList records)
	{
		if(mHeader.RequiresOwnerTags && mHeader.VersionMajor >= 2)
			throw new IllegalStateException("Owner tags are required. You can only append records through appendRecords(records, tag)");
		
		synchronized (mAsyncService)
		{
			LogUtil.finest("Submitting appendRecords async task");
			return mAsyncService.submit(new AppendRecordsTask(this, records));
		}
	}
	public Future<Boolean> appendRecordsAsync(RecordList records, String owner)
	{
		assert mHeader.VersionMajor >= 2 : "Owner tags are only suppored in version 2 and above";
		synchronized (mAsyncService)
		{
			LogUtil.finest("Submitting appendRecords async task");
			
			return mAsyncService.submit(new AppendRecordsTask(this, records, owner));
		}
	}
	
	private int initialiseSession(RecordList records, boolean absolute) throws IOException
	{
		assert mIsLoaded;
		assert records.size() > 0;
		
		LogUtil.fine("Initializing New Session with " + records.size() + " records");
		mLock.writeLock().lock();

		
		try
		{
			IndexEntry session = new IndexEntry();
			session.RecordCount = (short) records.size();
	
			// Calculate the expected size
			int totalSize = 0;
			for(Record record : records)
				totalSize += record.getSize(absolute);
			
			LogUtil.finer(" Total size of records: " + totalSize);
			LogUtil.finer(" Actual session size: " + Math.max(totalSize, DesiredMaximumSessionSize));
			
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
					LogUtil.severe(record.getType().toString() + " is returning incorrect size. Expected: " + expectedSize + " got " + actualSize);
				}
				// Update deep mode
				if(record instanceof SessionInfoRecord)
				{
					mDeepMode = ((SessionInfoRecord)record).isDeep();
				}
				lastSize = stream.size();
			}
			
			// Ensure i didnt mess up the implementation of getSize()
			assert bstream.size() == totalSize;
			
			LogUtil.finest(" Produced byte stream");
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
				
				LogUtil.finest(" writing byte stream to file");
				mFile.seek(session.Location);
				mFile.write(bstream.toByteArray());
				LogUtil.finest(" done");
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
				mFile.write(bstream.toByteArray());
			}
			
			
			// Write the index entry
			int id = addSession(session);
			
			if(!CrossReferenceIndex.instance.addSession(this, session, records.getAllChunks()))
				LogUtil.warning("Failed to add session to xreference");
			else
				LogUtil.finer("Added session to cross reference");
			
			return id;
		}
		finally
		{
			mLock.writeLock().unlock();
		}
	}

	/**
	 * Purges all records between the fromDate inclusive, and the toDate exclusive
	 */
	public boolean purgeRecords(long fromDate, long toDate)
	{
		assert mIsLoaded;
		
		boolean result;
		mLock.writeLock().lock();
		try
		{
			LogUtil.info("Purging records from " + Util.dateToString(fromDate) + " to " + Util.dateToString(toDate));
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
			LogUtil.finer("  " + relevantEntries.size() + " Matching Sessions");
			
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
						mActiveSessions.put(otag, null);

					// So that anything else using this object through a reference, wont do any damage
					entry.RecordCount = 0;
					entry.Location = 0;
					entry.Id = -1;
					entry.TotalSize = 0;
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
					sessionData.splitRecords(endIndex, true);
					sessionData.removeBefore(startIndex);
					
					// Write back the new updated data
					if(sessionData.size() == 0)
					{
						removeSession(sessionIndex);
						
						if(mActiveSessions.get(otag) != null && mActiveSessions.get(otag) == entry.Id)
							mActiveSessions.put(otag, null);
						
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
								LogUtil.severe(record.getType().toString() + " is returning incorrect size. Expected: " + expectedSize + " got " + actualSize);
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
					}
				}
			}
			result = true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			result = false;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			result = false;
		}
		
		mLock.writeLock().unlock();
		return result;
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
			e.printStackTrace();
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
		
		return merged;
	}
	private void addHole(HoleEntry entry) throws IOException
	{
		if(entry.Size == 0)
			return;
		
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
					
					LogUtil.finer("Merging Hole");
					
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
				
				LogUtil.finest("Hole inserted into padding");
			}
			else
			{
				// See if there is a hole we can extend into
				int hole = isRoomFor(HoleEntry.cSize,mHeader.HolesIndexLocation + mHeader.HolesIndexSize);
				
				if(hole == -1 || (hole != -1 && mHoleIndex.get(hole).AttachedTo != null)) 
				{
					// There isnt a hole appended to the index
					// Relocate the holes index
					HoleEntry oldIndexHole = new HoleEntry();
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

					LogUtil.finer("Hole index relocated");
					
					if(targetHole != -1) // Found a hole big enough
						// Consume the hole
						fillHole(targetHole,mHeader.HolesIndexLocation,newSize);
				}
				else
				{
					// There was a hole to use
					mFile.seek(mHeader.HolesIndexLocation + mHoleIndex.size() * HoleEntry.cSize);
					for(int i = index; i < mHoleIndex.size(); i++)
						mHoleIndex.get(i).write(mFile);

					
					mHeader.HolesIndexCount = mHoleIndex.size();
					mHeader.HolesIndexSize = mHoleIndex.size() * HoleEntry.cSize;
					
					LogUtil.finer("Hole inserted into hole");
					
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
		}
	}
	private void fillHole(int index, long start, long size) throws IOException
	{
		assert index >= 0 && index < mHoleIndex.size();
		
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
		assert index >= 0 && index < mHoleIndex.size();
		mLock.writeLock().lock();
		
		try
		{
			mFile.seek(mHeader.HolesIndexLocation + index * HoleEntry.cSize);
			mHoleIndex.set(index,entry);
			entry.write(mFile);
			LogUtil.finest("Updated hole");
		}
		finally
		{
			mLock.writeLock().unlock();
		}
	}
	private void removeHole(int index) throws IOException
	{
		assert index >= 0 && index < mHoleIndex.size();
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
			
			LogUtil.finer("Hole removed");
			// Write the file header
			mFile.seek(0);
			mHeader.write(mFile);
		}
		finally
		{
			mLock.writeLock().unlock();
		}
	}
	
	private int addSession(IndexEntry entry) throws IOException
	{
		int sessionIndex;
		
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
			
				
			if(hole == -1 || (hole != -1 && mHoleIndex.get(hole).AttachedTo != null))
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
				
				LogUtil.fine("Index relocated");
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
				
				LogUtil.finer("Session inserted");
				// Consume the hole
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
		}
		
		return sessionIndex;
	}
	private void removeSession(int index) throws IOException
	{
		assert index >= 0 && index < mIndex.size();
		
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
			
			LogUtil.finer("Session removed");
			// Write the file header
			mFile.seek(0);
			mHeader.write(mFile);
			
			addHole(hole);

			if(removedSession.OwnerTagId != -1)
			{
				removeOwnerMap(mOwnerTagMap.get(removedSession.OwnerTagId));
				rebuildTagMap();
			}
			
			rebuildIndexMap();
		}
		finally
		{
			mLock.writeLock().unlock();
		}
	}
	private void updateSession(int index, IndexEntry session) throws IOException
	{
		assert index >= 0 && index < mIndex.size() : "Tried to update session " + index + "/" + mIndex.size();
		mLock.writeLock().lock();
		
		try
		{
			mFile.seek(mHeader.IndexLocation + index * IndexEntry.cSize[mHeader.VersionMajor]);
			mIndex.set(index,session);
			session.write(mHeader.VersionMajor,mFile);
			
			LogUtil.finest("Session updated");
		}
		finally
		{
			mLock.writeLock().unlock();
		}
	}
	
	private int addOwnerMap(OwnerMapEntry map) throws IOException
	{
		int resultIndex;
		
		mLock.writeLock().lock();
		try
		{
			mOwnerTagList.add(map);
			
			// Check if there is room in the file for this
			int hole = isRoomFor(OwnerMapEntry.cSize,mHeader.OwnerMapLocation + mHeader.OwnerMapSize);
			
			if(hole == -1 || (hole != -1 && mHoleIndex.get(hole).AttachedTo != null))
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
				
				LogUtil.fine("Owner Map Index relocated");
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
				
				LogUtil.finer("Owner Map inserted");
				// Consume the hole
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
		}
		return resultIndex;
	}
	
	private void removeOwnerMap(int index) throws IOException
	{
		assert index >= 0 && index < mOwnerTagList.size();
		
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
			
			LogUtil.finer("Owner Map removed");
			// Write the file header
			mFile.seek(0);
			mHeader.write(mFile);
			
			addHole(hole);
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
		}
	}
	private void readHoles(RandomAccessFile file, FileHeader header) throws IOException
	{
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
					if(ent.Location == session.Location + session.TotalSize)
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
		}
	}
	private void readOwnerMap(RandomAccessFile file, FileHeader header) throws IOException
	{
		mLock.writeLock().lock();
		
		try
		{
			mOwnerTagList = new ArrayList<OwnerMapEntry>();
			LogUtil.info("Ownermap is assigned");
			
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
		}
	}
	private int mReferenceCount = 0;
	
	private boolean mIsLoaded = false;
	private boolean mIsClosing = false;
	private int mTimeoutId = -1;
	private String mPlayerName;
	private ArrayList<IndexEntry> mIndex;
	private HashMap<Integer, Integer> mIndexMap;
	
	private ArrayList<HoleEntry> mHoleIndex;
	
	private ArrayList<OwnerMapEntry> mOwnerTagList;
	private HashMap<Integer, Integer> mOwnerTagMap;
	
	private RandomAccessFile mFile;
	
	private HashMap<String, Integer> mActiveSessions;
	private FileHeader mHeader;
	private File mFilePath;
	private boolean mDeepMode;
	
	/// The byte size that sessions should be cut down to. This may or may not be met depending on the size of the records. 
	public static long DesiredMaximumSessionSize = 102400;

	public static boolean sNoTimeoutOverride = false;
	
	// Task for closing the logfile when everything is executed
	private class CloseTask implements Runnable 
	{
		@Override
		public void run() 
		{
			LogUtil.fine("Closing log for '" + mPlayerName + "'");
			
			try 
			{
				mFile.close();
			} 
			catch (IOException e) {}
			
			mIsLoaded = false;
			mIndex.clear();
			mIndex = null;
			mHoleIndex.clear();
			mHoleIndex = null;
			if(mOwnerTagList != null)
			{
				mOwnerTagList.clear();
				mOwnerTagList = null;
				LogUtil.info("Ownermap is removed");
			}
			LogUtil.finest("CloseTask is completed");
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
