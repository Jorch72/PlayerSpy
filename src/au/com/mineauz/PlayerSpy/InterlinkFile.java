package au.com.mineauz.PlayerSpy;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;


@SuppressWarnings( "unused" )
public class InterlinkFile 
{
	public static class Predicate
	{
		private long mStartTime = 0;
		private long mEndTime = Long.MAX_VALUE;
		private Location mLocation = null;
		private String mFilter = null;
		
		public Predicate startsAt(long time)
		{
			mStartTime = time;
			return this;
		}
		public Predicate endsAt(long time)
		{
			mEndTime = time;
			return this;
		}
		public Predicate containsLocation(Location location)
		{
			mLocation = location.clone();
			return this;
		}
		public Predicate filterBy(String filter)
		{
			mFilter = filter;
			return this;
		}
		
		public long getStartTime()
		{
			return mStartTime;
		}
		public long getEndTime()
		{
			return mEndTime;
		}
		
		public Location getLocation()
		{
			return mLocation;
		}
		public String getFilter()
		{
			return mFilter;
		}
	}
	
	/**
	 * Creates a new interlink file at the specified location
	 * @param file The location to create the file
	 * @return a new initialised interlink file that must be closed when done, or null if an error occurred
	 */
	public static InterlinkFile create(File file)
	{
		try
		{
			// Ensure the path exists
			if(file.getParentFile() != null)
				file.getParentFile().mkdirs();
			
			file.createNewFile();
			
			// Load up the file
			InterlinkFile linkFile = new InterlinkFile();
			linkFile.mFilePath = file;
			linkFile.mFile = new RandomAccessFile(file, "rw");
			
			// Write the default header
			linkFile.mHeader = new InterlinkHeader();
			linkFile.mHeader.write(linkFile.mFile);
			
			// Prepare some empty lists
			linkFile.mFileMap = new HashMap<Short, FileIndexEntry>();
			linkFile.mHoles = new ArrayList<HoleEntry>();
			
			linkFile.mHeader.FileLocation = linkFile.mFile.length();
			linkFile.mFiles = linkFile.new InterlinkFileLinkedList<FileIndexEntry>(linkFile.mFile, linkFile.mHeader.FileLocation, FileIndexEntry.class, true);
			linkFile.mHeader.ChunkSessionLocation = linkFile.mFile.length();
			linkFile.mChunks = linkFile.new InterlinkFileLinkedList<ChunkSessionEntry>(linkFile.mFile, linkFile.mHeader.ChunkSessionLocation, ChunkSessionEntry.class, true);
			linkFile.mHeader.SessionLocation = linkFile.mFile.length();
			linkFile.mSessions = linkFile.new InterlinkFileLinkedList<SessionIndexEntry>(linkFile.mFile, linkFile.mHeader.SessionLocation, SessionIndexEntry.class, true);
			
			// Allow io ops on the file
			linkFile.mIsLoaded = true;
			
			return linkFile;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * Loads an existing interlink file
	 * @param file The location of the interlink file
	 * @return True if the load was successful, false otherwise
	 */
	public synchronized boolean load(File file)
	{
		assert !mIsLoaded;
		
		try
		{
			mFilePath = file;
			mFile = new RandomAccessFile(file, "rw");
			
			// Load the file header
			mHeader = new InterlinkHeader();
			mHeader.read(mFile);
			
			// Load the lists
			loadHoleList();

			mFiles = new InterlinkFileLinkedList<FileIndexEntry>(mFile, mHeader.FileLocation, FileIndexEntry.class, false);
			mChunks = new InterlinkFileLinkedList<ChunkSessionEntry>(mFile, mHeader.ChunkSessionLocation, ChunkSessionEntry.class, false);
			mSessions = new InterlinkFileLinkedList<SessionIndexEntry>(mFile, mHeader.SessionLocation, SessionIndexEntry.class, false);
			
			loadFileList();
			
			if(!mSessions.isEmpty())
			{
				SessionIndexEntry last = mSessions.last();
				if(last != null)
					SessionIndexEntry.sNextId = last.Id + 1;
			}
			
			// Allow io ops on the file
			mIsLoaded = true;
			
			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * Closes the interlink file
	 */
	public synchronized void close()
	{
		assert mIsLoaded;
		
		try
		{
			mFile.close();
			
			// Release the maps/lists
			mFileMap.clear();
			mFileMap = null;
			
			mHoles.clear();
			mHoles = null;
			
			mIsLoaded = false;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Gets all session that match the specified predicate
	 */
	public List<Pair<String, Integer>> getSessionsMatching(Predicate predicate)
	{
		assert mIsLoaded;
		return null;
	}
	
	/**
	 * Adds a session to the interlink registry
	 * @param log The log file the session is in
	 * @param session The session to add
	 * @param owner The owner tag for a session. can be null
	 * @param allChunks All the chunks present in the session
	 * @return True if it was added, false if an io error occurred
	 */
	public synchronized boolean addSession(LogFile log, IndexEntry session, String owner, List<Chunk> allChunks)
	{
		assert mIsLoaded;
		try
		{
			// Add or get the file id
			short fileId = addFile(log);

			SessionIndexEntry newEntry = new SessionIndexEntry();
			newEntry.File = fileId;
			newEntry.Id = SessionIndexEntry.sNextId++;
			newEntry.OwnerTag = owner;
			newEntry.SessionIndex = log.getSessions().indexOf(session);
			newEntry.StartTime = session.StartTimestamp;
			newEntry.EndTime = session.EndTimestamp;
			
			mSessions.add(newEntry);
			
			addChunks(allChunks, newEntry.Id);
			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * Removes a session from the interlink registry
	 * @param log The log file the session is in
	 * @param session The session to remove
	 * @return True if it was removed, false if it didnt exist or an io error occurred
	 */
	public synchronized boolean removeSession(LogFile log, IndexEntry session)
	{
		assert mIsLoaded;
		// TODO: removeSession()
		return false;
	}
	
	/**
	 * Updates a session in the interlink registry
	 * @param log The log file the session is in
	 * @param session The session to update
	 * @param allChunks all chunks present in the update, only new chunks will be added
	 * @return True if the update was successful, false if the session didnt exist, or an io error occurred
	 */
	public synchronized boolean updateSession(LogFile log, IndexEntry session, List<Chunk> allChunks)
	{
		assert mIsLoaded;
		
		short fileId = -1;
		int sessionIndex = log.getSessions().indexOf(session);
		
		for(Map.Entry<Short, FileIndexEntry> entry : mFileMap.entrySet())
		{
			if(entry.getValue().LogName.equalsIgnoreCase(log.getName()))
			{
				fileId = entry.getKey();
				break;
			}
		}
		if(fileId == -1 || sessionIndex == -1)
		{
			return false;
		}
			
		
		SessionIndexEntry sessionEntry;
		SessionComparer comparer = new SessionComparer(fileId, sessionIndex);
		
		Pair<Integer, SessionIndexEntry> result = mSessions.get(comparer);
		int index = result.getArg1();
		if(index == -1)
		{
			LogUtil.fine("Failed to get session using comparer");
			return false;
		}
		
		sessionEntry = result.getArg2();
		sessionEntry.StartTime = session.StartTimestamp;
		sessionEntry.EndTime = session.EndTimestamp;
		
		if(mSessions.set(index, sessionEntry) == null)
		{
			LogUtil.fine("Failed to set session");
			return false;
		}

		// Find what chunks need to be removed from the list to add
		for(ChunkSessionEntry chunkSession : mChunks)
		{
			for(Chunk chunk : allChunks)
			{
				if(chunkSession.X == chunk.getX() && chunkSession.Z == chunk.getZ() && chunkSession.WorldId.equals(chunk.getWorld().getUID()))
				{
					allChunks.remove(chunk);
					break;
				}
			}
		}
		
		// Add the new chunks
		try {
			addChunks(allChunks, sessionEntry.Id);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		
		return true;
	}
	/**
	 * Updates the indices so that they point correctly
	 * @param log The logfile the session is in
	 * @param oldIndex The index the session was in
	 * @param newIndex The index the session is in
	 * @return True if the move succeeded
	 */
	public synchronized boolean moveSession(LogFile log, int oldIndex, int newIndex)
	{
		short fileId = -1;
		
		for(Map.Entry<Short, FileIndexEntry> entry : mFileMap.entrySet())
		{
			if(entry.getValue().LogName.equalsIgnoreCase(log.getName()))
			{
				fileId = entry.getKey();
				break;
			}
		}
		if(fileId == -1)
			return false;
		
		Pair<Integer, SessionIndexEntry> data = mSessions.get(new SessionComparer(fileId, oldIndex));
		if(data.getArg1() == -1)
			return false;
		
		if(!mSessions.remove(data.getArg1()))
			return false;
		
		if(!mSessions.add(data.getArg2()))
			return false;
		
		return true;
	}
	
	private void loadFileList() throws IOException
	{
		FileIndexEntry.sNextId = -1;
		mFileMap = new HashMap<Short, FileIndexEntry>();
		
		for(FileIndexEntry ent : mFiles)
		{
			FileIndexEntry.sNextId = (short)Math.max(FileIndexEntry.sNextId, ent.Id);
			mFileMap.put(ent.Id, ent);
		}
		FileIndexEntry.sNextId++;
	}
	private void loadHoleList() throws IOException
	{
		mFile.seek(mHeader.HoleLocation);
		mHoles = new ArrayList<HoleEntry>();
		
		for(int i = 0; i < mHeader.HoleCount; i++)
		{
			HoleEntry ent = new HoleEntry();
			ent.read(mFile);
			
			mHoles.add(ent);
		}
	}
	
	/**
	 * Adds the listed chunks to the file but only if they have not already been added 
	 * @param chunks
	 * @param sessionId
	 * @throws IOException
	 */
	private void addChunks(List<Chunk> chunks, int sessionId) throws IOException
	{
		for(Chunk chunk : chunks)
		{
			ChunkSessionEntry newEntry = new ChunkSessionEntry(chunk, sessionId);
			
			mChunks.add(newEntry);
		}
	}
	private void removeAllChunks(int sessionId)
	{
		
	}
	/**
	 * Gets the index of the hole that is available at that location that can fit that amount
	 * @param size The amount of bytes needed to fit
	 * @param atLocation The location where its needed
	 * @return The index of the hole or -1 if there isnt one. The index can be == to the number of holes meaning the end of the file
	 */
	private int getHoleToFit(long size, long atLocation)
	{
		try
		{
			long fileLength = mFile.length();
			
			int index = 0;
			for(HoleEntry hole : mHoles)
			{
				if(hole.Location <= atLocation && hole.Location + hole.Size >= atLocation)
				{
					// Check that there is enough room
					if((hole.Location + hole.Size) - (size - atLocation) >= 0)
						return index;
					else
						break;
				}
				++index;
			}
			
			if(atLocation >= fileLength)
				return mHoles.size();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return -1;
	}
	/**
	 * Gets the index of the hole that is available anywhere that can fit that amount
	 * @param size The amount of bytes needed to fit
	 * @return The index of the hole or -1 if there isnt one. The index can be == to the number of holes meaning the end of the file
	 */
	private int getHoleToFit(long size)
	{
		int index = 0;
		for(HoleEntry hole : mHoles)
		{
			// Check that there is enough room
			if(hole.Size >= size)
				return index;
			++index;
		}
		
		return -1;
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
	
	private void addHole(HoleEntry entry)
	{
		if(entry.Size == 0)
			return;
		try
		{
			// Check if we need to merge it
			for(int i = 0; i < mHoles.size(); i++)
			{
				HoleEntry existing = mHoles.get(i);
				if(canMergeHoles(entry,existing))
				{
					HoleEntry newHole = mergeHoles(entry,existing);
					mHoles.set(i, newHole);
					
					// Write the changes
					mFile.seek(mHeader.HoleLocation + i * HoleEntry.cSize);
					newHole.write(mFile);
					
					LogUtil.finer("Merging Hole");
					
					return;
				}
			}
			
			int index = 0;
			for(index = 0; index < mHoles.size(); index++)
			{
				if(mHoles.get(index).Location > entry.Location)
					break;
			}
			mHoles.add(index,entry);
			
			// It must be added
			if(mHeader.HolePadding >= HoleEntry.cSize)
			{
				// there is padding availble to use 
				mFile.seek(mHeader.HoleLocation + index * HoleEntry.cSize);
				// Shift the entries
				for(int i = index; i < mHoles.size(); i++)
					mHoles.get(i).write(mFile);
				
				mHeader.HoleCount = mHoles.size();
				mHeader.HoleSize = mHoles.size() * HoleEntry.cSize;
				mHeader.HolePadding -= HoleEntry.cSize;
				
				LogUtil.finest("Hole inserted into padding");
			}
			else
			{
				// See if there is a hole we can extend into
				int hole = getHoleToFit(HoleEntry.cSize,mHeader.HoleLocation + mHeader.HoleSize);
				
				if(hole == -1 || (hole != -1 && mHoles.get(hole).AttachedTo != null)) 
				{
					// There isnt a hole appended to the index
					// Relocate the holes index
					HoleEntry oldIndexHole = new HoleEntry();
					oldIndexHole.Location = mHeader.HoleLocation;
					oldIndexHole.Size = mHeader.HoleSize + mHeader.HolePadding;
					// Try to merge it
					boolean merged = false;
					for(int i = 0; i < mHoles.size(); i++)
					{
						if(canMergeHoles(oldIndexHole,mHoles.get(i)))
						{
							mHoles.set(i, mergeHoles(oldIndexHole,mHoles.get(i)));
							
							if(merged)
								mHoles.remove(oldIndexHole);
							
							oldIndexHole = mHoles.get(i);
							merged = true;
						}
					}
					
					// Add it now if it wasnt merged
					if(!merged)
					{
						int index2 = 0;
						for(index2 = 0; index2 < mHoles.size(); index2++)
						{
							if(mHoles.get(index2).Location > oldIndexHole.Location)
								break;
						}
						mHoles.add(index2,oldIndexHole);
					}
					
					mHeader.HoleLocation = mFile.length();
					
					// The total size of the hole index now including 1 extra entry as padding
					long newSize = (mHoles.size() + 1) * HoleEntry.cSize;
					
					// Check all the holes to see if there is any space
					int targetHole = -1;
					for(int i = 0; i < mHoles.size(); i++)
					{
						if(mHoles.get(i).Size >= newSize)
						{
							targetHole = i;
							mHeader.HoleLocation = mHoles.get(targetHole).Location;
							break;
						}
					}
					
					// Prepare the header info
					mHeader.HolePadding = HoleEntry.cSize;
					mHeader.HoleCount = mHoles.size();
					mHeader.HoleSize = mHoles.size() * HoleEntry.cSize;
					
					// Write the items
					mFile.seek(mHeader.HoleLocation);
					for(int i = 0; i < mHoles.size(); i++)
						mHoles.get(i).write(mFile);
					
					// Padding
					mFile.write(new byte[HoleEntry.cSize]);

					LogUtil.finer("Hole index relocated");
					
					if(targetHole != -1) // Found a hole big enough
						// Consume the hole
						fillHole(targetHole,mHeader.HoleLocation,newSize);
				}
				else
				{
					// There was a hole to use
					mFile.seek(mHeader.HoleLocation + mHoles.size() * HoleEntry.cSize);
					for(int i = index; i < mHoles.size(); i++)
						mHoles.get(i).write(mFile);

					
					mHeader.HoleCount = mHoles.size();
					mHeader.HoleSize = mHoles.size() * HoleEntry.cSize;
					
					LogUtil.finer("Hole inserted into hole");
					
					fillHole(hole,mFile.getFilePointer() - HoleEntry.cSize,HoleEntry.cSize);
				}
			}
			
			// Write the file header
			mFile.seek(0);
			mHeader.write(mFile);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void fillHole(int index, long start, long size)
	{
		assert index >= 0 && index < mHoles.size();
		
		HoleEntry hole = mHoles.get(index);
		
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
	private void updateHole(int index, HoleEntry entry)
	{
		assert index >= 0 && index < mHoles.size();
		
		try
		{
			mFile.seek(mHeader.HoleLocation + index * HoleEntry.cSize);
			mHoles.set(index,entry);
			entry.write(mFile);
			LogUtil.finest("Updated hole");
		}
		catch(IOException e)
		{
			
		}
	}
	private void removeHole(int index)
	{
		assert index >= 0 && index < mHoles.size();
		try
		{
			if(index != mHoles.size()-1)
			{
				// shift entries
				mFile.seek(mHeader.HoleLocation + index * HoleEntry.cSize);
				for(int i = index+1; i < mHoles.size(); i++)
					mHoles.get(i).write(mFile);
			}
			
			// Clear the last entry
			mFile.seek(mHeader.HoleLocation + (mHoles.size()-1) * HoleEntry.cSize);
			mFile.write(new byte[HoleEntry.cSize]);
			
			mHoles.remove(index);
			// Increase the padding
			mHeader.HolePadding += HoleEntry.cSize;
			mHeader.HoleCount = mHoles.size();
			mHeader.HoleSize = mHoles.size() * HoleEntry.cSize;
			
			LogUtil.finer("Hole removed");
			// Write the file header
			mFile.seek(0);
			mHeader.write(mFile);
		}
		catch(IOException e)
		{
			
		}
	}
	
	/**
	 * Adds a log file and returns the id
	 * @param log The log to add
	 * @return The id of it
	 */
	private short addFile(LogFile log)
	{
		assert mIsLoaded;
		
		// Check that its not already added
		for(FileIndexEntry ent : mFileMap.values())
		{
			if(ent.LogName.equalsIgnoreCase(log.getName()))
				return ent.Id;
		}
		LogUtil.info("Adding new file" + log.getName());
		// Prepare the entry
		FileIndexEntry fileEnt = new FileIndexEntry();
		fileEnt.LogName = log.getName();
		fileEnt.Id = FileIndexEntry.sNextId++;
		
		mFileMap.put(fileEnt.Id, fileEnt);
		mFiles.add(fileEnt);
		return fileEnt.Id;
	}
	private boolean removeFile(LogFile log)
	{
		return false;
//		try
//		{
//			boolean removed = false;
//			// Check that it exists and remove it
//			for(FileIndexEntry ent : mFileMap.values())
//			{
//				if(ent.LogName.equalsIgnoreCase(log.getName()))
//				{
//					mFileMap.remove(ent.Id);
//					removed = true;
//					break;
//				}
//			}
//			
//			if(!removed)
//				return false;
//			
//			
//			// Write the list
//			mFile.seek(mHeader.FileLocation);
//			for(FileIndexEntry ent : mFileMap.values())
//				ent.write(mFile);
//
//			// add the hole
//			HoleEntry oldHole = new HoleEntry();
//			oldHole.Location = mHeader.FileLocation + mHeader.FileSize;
//			oldHole.Size = FileIndexEntry.cSize;
//
//			addHole(oldHole);
//			
//			// Update the header values
//			mHeader.FileSize = mFile.getFilePointer() - mHeader.FileLocation;
//			mHeader.FileCount = mFileMap.size();
//
//			mHeader.write(mFile);
//			return true;
//		}
//		catch(IOException e)
//		{
//			e.printStackTrace();
//			return false;
//		}
	}
	
	/**
	 * Gets a list of sessionId's that have something for that chunk
	 */
	private List<Integer> getSessionsMatchingChunks(Chunk chunk)
	{
		ChunkComparer comparer = new ChunkComparer(chunk);
		
		ArrayList<Integer> sessionIds = new ArrayList<Integer>();
		
		for(ChunkSessionEntry entry : mChunks)
		{
			if(comparer.compareTo(entry) == 0)
				sessionIds.add(entry.SessionId);
		}
		
		return sessionIds;
	}
	
	/**
	 * Gets the session entry from an id
	 */
	private SessionIndexEntry getSessionEntry(int id)
	{
		// TODO: create some magic way of quickly finding a session by id
		for(SessionIndexEntry entry : mSessions)
		{
			if(entry.Id == id)
				return entry;
		}
		
		return null;
	}
	
	// The header
	private InterlinkHeader mHeader;
	
	// A map between file ids and the file entry
	private HashMap<Short, FileIndexEntry> mFileMap;
	
	// All the gaps in the file
	private ArrayList<HoleEntry> mHoles;
	
	private InterlinkFileLinkedList<ChunkSessionEntry> mChunks;
	private InterlinkFileLinkedList<SessionIndexEntry> mSessions;
	private InterlinkFileLinkedList<FileIndexEntry> mFiles;
	
	// The opened file
	private RandomAccessFile mFile;
	private boolean mIsLoaded = false;
	// The path of the opened file
	private File mFilePath;
	
	private class ChunkComparer implements Comparable<ChunkSessionEntry>
	{
		private Chunk mChunk;
		public ChunkComparer(Chunk chunk)
		{
			mChunk = chunk;
		}
		@Override
		public int compareTo(ChunkSessionEntry other) 
		{
			if(other.WorldId.equals(mChunk.getWorld().getUID()))
			{
				if(other.X == mChunk.getX() && other.Z == mChunk.getZ())
					return 0; // Exact match
			}
			
			// Not exact match
			int otherHash = other.hashCode();
			UUID worldId = mChunk.getWorld().getUID();
			int hash = mChunk.getX() + mChunk.getZ() + (int)(worldId.getMostSignificantBits() >> 32) / 100 + (int)(worldId.getMostSignificantBits() & -1) / 100 + (int)(worldId.getLeastSignificantBits() >> 32) / 100 + (int)(worldId.getLeastSignificantBits() & -1) / 100;
			
			return (hash < otherHash ? -1 : 1);
		}
		
	}
	private class SessionComparer implements Comparable<SessionIndexEntry>
	{
		private short mFileId;
		private int mSessionIndex;
		
		public SessionComparer(short fileId, int sessionIndex)
		{
			mFileId = fileId;
			mSessionIndex = sessionIndex;
		}
		
		@Override
		public int compareTo(SessionIndexEntry other) 
		{
			int hash = (mFileId << 16) | (mSessionIndex);
			int otherHash = other.hashCode();
			
			if(hash < otherHash)
				return -1;
			if(hash > otherHash)
				return 1;
			return 0;
		}
		
	}
	private class InterlinkFileLinkedList<E extends IWritable> extends FileLinkedList<E>
	{
		int mHole = -1;
		public InterlinkFileLinkedList(RandomAccessFile file, long location, Class<? extends E> itemClass, boolean isNew) throws IOException 
		{
			super(file, location, itemClass, isNew);
		}

		@Override
		protected long onRequestSpace(long size) throws IOException 
		{
			mHole = getHoleToFit(size);
			if(mHole != -1)
				return mHoles.get(mHole).Location;
			else
			{
				return mFile.length();
			}
		}

		@Override
		protected void onUseSpace(long location, long size) throws IOException
		{
			if(mHole != -1)
				fillHole(mHole,location, size);
		}

		@Override
		protected void onReliquishSpace(long location, long size) throws IOException
		{
			HoleEntry oldHole = new HoleEntry();
			oldHole.Location = location;
			oldHole.Size = size;
			addHole(oldHole);
		}
		
	}
}

class InterlinkHeader
{
	public static final int cSize = 78;
	
	public byte VersionMajor = 1;
	public byte VersionMinor = 0;
	
	public long FileLocation = cSize;
	
	public long SessionLocation = cSize;
	
	public long ChunkSessionLocation = cSize;
	
	public int HoleCount = 0;
	public long HoleLocation = cSize;
	public long HoleSize = 0;
	public long HolePadding = 0;
	
	public byte[] Reserved = new byte[48];
	
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeByte(VersionMajor);
		file.writeByte(VersionMinor);
		
		file.writeInt((int)FileLocation);
		
		file.writeInt((int)SessionLocation);
		
		file.writeInt((int)ChunkSessionLocation);
		
		file.writeInt(HoleCount);
		file.writeInt((int)HoleLocation);
		file.writeInt((int)HoleSize);
		file.writeInt((int)HolePadding);
		
		file.write(Reserved);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		VersionMajor = file.readByte();
		VersionMinor = file.readByte();
		
		if(VersionMajor != 1 || VersionMinor != 0)
			throw new IOException("Invalid interlinkheader version: " + VersionMajor + "." + VersionMinor);
		
		FileLocation = file.readInt();
		
		SessionLocation = file.readInt();
		
		ChunkSessionLocation = file.readInt();
		
		HoleCount = file.readInt();
		HoleLocation = file.readInt();
		HoleSize = file.readInt();
		HolePadding = file.readInt();
		
		file.readFully(Reserved);
	}
}

class FileIndexEntry implements IWritable
{
	public static final int cSize = 32;
	public static short sNextId = 0;
	public short Id;
	public String LogName;
	
	@Override
	public int hashCode() 
	{
		return LogName.hashCode();
	}
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeShort(Id);
		byte[] data = new byte[30];
		for(int i = 0; i < LogName.length() && i < 30; i++)
		{
			data[i] = (byte)LogName.charAt(i);
		}
		
		file.write(data);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		Id = file.readShort();
		
		char[] data = new char[30];
		for(int i = 0; i < 30; i++)
			data[i] = (char)file.readByte();
		
		LogName = String.valueOf(data);
		if(LogName.indexOf(0) != -1)
			LogName = LogName.substring(0, LogName.indexOf(0));
	}

	@Override
	public int getSize() 
	{
		return cSize;
	}
}

class SessionIndexEntry implements IWritable
{
	public static int sNextId = 0;
	public static final int cSize = 42; 
	public int Id;
	public short File;
	public String OwnerTag;
	public int SessionIndex;
	public long StartTime;
	public long EndTime;
	
	public List<ChunkSessionEntry> Chunks;
	
	@Override
	public int hashCode() 
	{
		return  (File << 16) | (SessionIndex);
		//return (int)(EndTime >> 32) | (int)(EndTime);
		//return File << 16 | SessionIndex;
	}
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeInt(Id);
		file.writeShort(File);
		byte[] data = new byte[16];
		if(OwnerTag != null)
		{
			for(int i = 0; i < OwnerTag.length() && i < data.length; i++)
				data[i] = (byte)OwnerTag.charAt(i);
		}
		file.write(data);
		
		file.writeInt(SessionIndex);
		file.writeLong(StartTime);
		file.writeLong(EndTime);
	}
	public void read(RandomAccessFile file) throws IOException
	{
		Id = file.readInt();
		File = file.readShort();
		
		char[] data = new char[16];
		for(int i = 0; i < data.length; i++)
			data[i] = (char)file.readByte();
		
		OwnerTag = String.valueOf(data);
		if(OwnerTag.indexOf(0) != -1)
			OwnerTag = OwnerTag.substring(0, OwnerTag.indexOf(0));
		if(OwnerTag.length() == 0)
			OwnerTag = null;
		
		SessionIndex = file.readInt();
		
		StartTime = file.readLong();
		EndTime = file.readLong();
	}
	@Override
	public int getSize() 
	{
		return cSize;
	}
}

class ChunkSessionEntry implements IWritable
{
	public static final int cSize = 28;
	public ChunkSessionEntry()
	{
		
	}
	public ChunkSessionEntry(Chunk chunk, int sessionId)
	{
		X = chunk.getX();
		Z = chunk.getZ();
		WorldId = chunk.getWorld().getUID();
		SessionId = sessionId;
	}
	public int X;
	public int Z;
	
	public World getWorld()
	{
		return Bukkit.getWorld(WorldId);
	}
	public void setWorld(World world)
	{
		WorldId = world.getUID();
	}
	public UUID WorldId;
	
	public int SessionId;
	
	public Chunk getChunk()
	{
		World world = getWorld();
		if(world == null)
			return null;
		else
			return world.getChunkAt(X,Z);
	}
	
	@Override
	public int hashCode()
	{
		return X + Z + (int)(WorldId.getMostSignificantBits() >> 32) / 100 + (int)(WorldId.getMostSignificantBits() & -1) / 100 + (int)(WorldId.getLeastSignificantBits() >> 32) / 100 + (int)(WorldId.getLeastSignificantBits() & -1) / 100;
	}
	
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeInt(X);
		file.writeInt(Z);
		
		file.writeLong(WorldId.getMostSignificantBits());
		file.writeLong(WorldId.getLeastSignificantBits());
		
		file.writeInt(SessionId);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		X = file.readInt();
		Z = file.readInt();
		
		WorldId = new UUID(file.readLong(), file.readLong());
		
		SessionId = file.readInt();
	}
	@Override
	public int getSize() 
	{
		return cSize;
	}
}

