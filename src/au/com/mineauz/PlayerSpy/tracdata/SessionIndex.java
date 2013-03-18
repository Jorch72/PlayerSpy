package au.com.mineauz.PlayerSpy.tracdata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.ILocationAware;
import au.com.mineauz.PlayerSpy.Records.IPlayerLocationAware;
import au.com.mineauz.PlayerSpy.Records.IRollbackable;
import au.com.mineauz.PlayerSpy.Records.InventoryRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Records.SessionInfoRecord;
import au.com.mineauz.PlayerSpy.Records.TeleportRecord;
import au.com.mineauz.PlayerSpy.Records.WorldChangeRecord;
import au.com.mineauz.PlayerSpy.Utilities.SafeChunk;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.debugging.Profiler;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;

public class SessionIndex extends DataIndex<SessionEntry, IMovableData<SessionEntry>>
{
	static int NextId = (int)(System.currentTimeMillis() / 1000);
	private HashMap<Integer, Integer> mSessionMap = new HashMap<Integer, Integer>();
	private HashMap<String, Integer> mActiveSessions = new HashMap<String, Integer>();
	
	private static long mInitialSessionSize = 102400;
	
	/// These are used to maintain state between non ownertagged sessions
	private InventoryRecord mLastInventory;
	private Location mLastLocation;
	private boolean mDeepMode;
	
	public SessionIndex( LogFile log, FileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(log, header, file, locator);
	}

	@Override
	public String getIndexName()
	{
		return "Session Index";
	}

	@Override
	public long getLocation()
	{
		return mHeader.IndexLocation;
	}

	@Override
	public long getSize()
	{
		return mHeader.IndexSize;
	}

	@Override
	protected int getEntrySize()
	{
		return SessionEntry.cSize[mHeader.VersionMajor];
	}

	@Override
	protected SessionEntry createNewEntry()
	{
		SessionEntry ent = new SessionEntry();
		ent.version = mHeader.VersionMajor;
		return ent;
	}

	@Override
	protected int getElementCount()
	{
		return mHeader.SessionCount;
	}

	@Override
	protected void updateElementCount( int newCount )
	{
		mHeader.SessionCount = newCount;
	}

	@Override
	protected void updateSize( long newSize )
	{
		mHeader.IndexSize = newSize;
	}

	@Override
	protected void updateLocation( long newLocation )
	{
		mHeader.IndexLocation = newLocation;
	}

	private void rebuildSessionMap()
	{
		mSessionMap.clear();
		
		int index = 0;
		for(SessionEntry entry : mElements)
		{
			mSessionMap.put(entry.Id, index);
			++index;
		}
	}
	
	@Override
	public void read() throws IOException
	{
		super.read();
		
		rebuildSessionMap();
		
		mActiveSessions.clear();
		
		// Find the active sessions
		for(int i = 0; i < mElements.size(); ++i)
		{
			SessionEntry session = get(i);
			if(session.Compressed)
				continue;
			
			String ownerTag = mLog.getOwnerTag(session);
			if(mActiveSessions.containsKey(ownerTag))
			{
				SessionEntry other = getSessionFromId(mActiveSessions.get(ownerTag));
				
				if(session.EndTimestamp > other.EndTimestamp)
					mActiveSessions.put(ownerTag, session.Id);
			}
			else
				mActiveSessions.put(ownerTag, session.Id);
		}
	}
	
	@Override
	public int add( SessionEntry entry ) throws IOException
	{
		entry.version = mHeader.VersionMajor;
		entry.Id = NextId++;
		
		int index = super.add(entry);
		
		rebuildSessionMap();
		
		// Update the location filter for the file
		mHeader.TotalLocationFilter.add(entry.ChunkLocationFilter);
		mFile.seek(0);
		mHeader.write(mFile);
		
		return index;
	}
	
	public SessionData addEmptySession() throws IOException
	{
		Debug.fine("Initializing new blank session");
		
		// Calculate size and prepare index entry
		SessionEntry session = new SessionEntry();
		session.Location = 0;
		session.RecordCount = 0;
		session.TotalSize = mInitialSessionSize;
		session.Padding = mInitialSessionSize;
		session.Compressed = false;
		
		session.StartTimestamp = 0;
		session.EndTimestamp = 0;
		
		// Find a location and ensure the space exists
		session.Location = mLocator.findFreeSpace(mInitialSessionSize);
		
		mFile.seek(session.Location + mInitialSessionSize - 1);
		mFile.writeByte(0);
		
		mLocator.consumeSpace(session.Location, mInitialSessionSize);
		
		// Write the index entry
		add(session);
		
		if(!CrossReferenceIndex.instance.addSession(mLog, session, new ArrayList<SafeChunk>()))
			Debug.warning("Failed to add session to xreference");
		else
			Debug.finer("Added session to cross reference");

		return getDataFor(session);
	}
	
	@Override
	protected void onRemove( SessionEntry entry )
	{
		entry.version = mHeader.VersionMajor;
		CrossReferenceIndex.instance.removeSession(mLog, entry);
	}
	
	@Override
	public void remove( int index ) throws IOException
	{
		super.remove(index);
		
		rebuildSessionMap();
		
		rebuildChunkFilters();
	}
	
	@Override
	protected int getInsertIndex( SessionEntry entry )
	{
		int insertIndex = 0;
		for(insertIndex = 0; insertIndex < mElements.size(); insertIndex++)
		{
			if(mElements.get(insertIndex).StartTimestamp > entry.StartTimestamp)
				break;
		}
		return insertIndex;
	}
	
	@Override
	public void set( int index, SessionEntry entry ) throws IOException
	{
		super.set(index, entry);
		
		// Update the location filter for the file
		mHeader.TotalLocationFilter.add(entry.ChunkLocationFilter);
		mFile.seek(0);
		mHeader.write(mFile);
	}
	
	public void rebuildChunkFilters() throws IOException
	{
		mHeader.TotalLocationFilter.clear();
		
		for(SessionEntry session : this)
			mHeader.TotalLocationFilter.add(session.ChunkLocationFilter);
		
		mFile.seek(0);
		mHeader.write(mFile);
	}
	
	public SessionEntry getSessionFromId(int id)
	{
		if(mSessionMap.containsKey(id))
			return get(mSessionMap.get(id));
		
		return null;
	}
	
	public SessionEntry getActiveSessionFor(String ownerTag)
	{
		if(mActiveSessions.containsKey(ownerTag))
			return getSessionFromId(mActiveSessions.get(ownerTag));
		
		return null;
	}
	
	public void setActiveSession(String ownerTag, SessionEntry session)
	{
		if(session == null)
			mActiveSessions.remove(ownerTag);
		else
			mActiveSessions.put(ownerTag, session.Id);
	}

	@Override
	public SessionData getDataFor( SessionEntry entry )
	{
		return new SessionData(entry);
	}
	
	
	public class SessionData implements IMovableData<SessionEntry>
	{
		private final SessionEntry mSession;
		
		public SessionData(SessionEntry session)
		{
			mSession = session;
		}
		
		@Override
		public SessionEntry getIndexEntry()
		{
			return mSession;
		}
		
		@Override
		public long getLocation()
		{
			return mSession.Location;
		}
		@Override
		public void setLocation( long newLocation )
		{
			mSession.Location = newLocation;
		}
		

		@Override
		public long getSize()
		{
			return mSession.TotalSize;
		}

		public RecordList read() throws IOException
		{
			boolean isAbsolute = mSession.OwnerTagId != -1;
			
			Debug.finer("Loading Session %d", mSession.Id);
			
			RecordList records = new RecordList();
			
			// Read the raw session data
			mFile.seek(mSession.Location);

			byte[] sessionRaw = new byte[(int)mSession.TotalSize];
			mFile.read(sessionRaw);
			
			// make it available to use
			ByteArrayInputStream istream = new ByteArrayInputStream(sessionRaw);
			
			DataInputStream stream = null;
			
			// get the input stream 
			if(mSession.Compressed)
			{
				GZIPInputStream compressedInput = new GZIPInputStream(istream);
				stream = new DataInputStream(compressedInput);
			}
			else
				stream = new DataInputStream(istream);
			
			World lastWorld = null;
			boolean hadInv = false;
			// Load the records
			for(int i = 0; i < mSession.RecordCount; i++)
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
				record.sourceFile = mLog; 
				record.sourceEntry = mSession;
				record.sourceIndex = (short)i;
				
				// update the last world
				if(record instanceof IPlayerLocationAware && ((IPlayerLocationAware)record).isFullLocation())
					lastWorld = ((IPlayerLocationAware)record).getLocation().getWorld();
				else if(record instanceof WorldChangeRecord)
					lastWorld = ((WorldChangeRecord)record).getWorld();
				else if((lastWorld == null && record.getType() != RecordType.FullInventory && record.getType() != RecordType.EndOfSession) && !isAbsolute)
				{
					Debug.warning("Corruption in " + mLog.getName() + ".tracdata session " + mSession.Id + " found. Attempting to fix");
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
					Debug.warning("Issue detected with " + mLog.getName() + ".trackdata in session " + mSession.Id + ". No world has been set. Defaulting to main world");
					lastWorld = Bukkit.getWorlds().get(0);
					records.add(new WorldChangeRecord(lastWorld));
				}
				if(!hadInv && i > 3 && !isAbsolute)
				{
					Debug.warning("Issue detected with " + mLog.getName() + ".trackdata in session " + mSession.Id + ". No inventory state has been set. ");
					hadInv = true;
				}
				records.add(record);
			}
			
			// Load the rollback state info in
			short[] indices = mLog.mRollbackIndex.getRolledBackRecords(mSession);
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
			
			return records;
		}
		
		/**
		 * Compresses the session. It is expected that the session is not already compressed.
		 */
		public void compress() throws IOException
		{
			if (mSession.Compressed)
				throw new IllegalStateException("Session " + mSession.Id + " is already compressed.");
			
			Debug.info("Found more records to write in new session. Compressing session");
			
			byte[] sessionData = new byte[(int)(mSession.TotalSize - mSession.Padding)];
			Debug.finest("Reading %X->%X", mSession.Location, mSession.Location + sessionData.length - 1);
			mFile.seek(mSession.Location);
			mFile.readFully(sessionData);
			
			ByteArrayOutputStream ostream = new ByteArrayOutputStream();
			GZIPOutputStream compressor = new GZIPOutputStream(ostream);
			
			compressor.write(sessionData);
			compressor.finish();
			
			if(ostream.size() < (mSession.TotalSize - mSession.Padding))
			{
				Debug.fine("Compressed to %d from %d. Reduction of %.1f%%", ostream.size(), (mSession.TotalSize - mSession.Padding), ((mSession.TotalSize - mSession.Padding)-ostream.size()) / (double)(mSession.TotalSize - mSession.Padding) * 100F);
				
				mFile.seek(mSession.Location);
				mFile.write(ostream.toByteArray());
				
				long oldSize = mSession.TotalSize;
				
				mSession.TotalSize = ostream.size();
				mSession.Compressed = true;
				
				set(indexOf(mSession), mSession);
				
				mLocator.releaseSpace(mSession.Location + ostream.size(), oldSize - ostream.size());

				mLog.pullData(mSession.Location + ostream.size());
			}
			else
				Debug.fine("Compression cancelled as the result was larger than the original");
		}
		
		/**
		 * Appends records to this session. Not all records will fit, in which case a list of remaining records will be returned.
		 * @param records The records to add to the session.
		 * @return null if all were added, or a list of remaining records
		 */
		public RecordList append(RecordList records) throws IOException
		{
			Debug.info("Begining append of %d records to Session %d", records.size(), mSession.Id);
			
			// This will be populated if there are too many records to put into this session
			RecordList splitSession = null;
			boolean isAbsolute = mSession.OwnerTagId != -1;
			
			Profiler.beginTimingSection("appendRecordsInternal");
			
			ArrayList<Short> rolledBackEntries = new ArrayList<Short>();
			
			try
			{
				Debug.fine("*Remaining padding: " + mSession.Padding);
				
				// Calculate the size of the records
				long totalSize = 0;
				int cutoffIndex = 0;
				
				if(mSession.Compressed)
				{
					Debug.warning("Attempting to write to compressed session. Moving to new session");
					return records;
				}
				
				short index = 0;
				for(Record record : records)
				{
					int size = record.getSize(isAbsolute);
					
					if(totalSize + size > mSession.Padding)
					{
						// Split
						splitSession = records.splitRecords(cutoffIndex, true);
						break;
					}
					totalSize += size;
					cutoffIndex++;
					
					if(record instanceof IRollbackable)
					{
						if(((IRollbackable)record).wasRolledBack())
							rolledBackEntries.add((Short)(short)(index + mSession.RecordCount));
					}
					
					// Add any location to the location filter 
					if(record instanceof ILocationAware && !(record instanceof IPlayerLocationAware))
					{
						mSession.LocationFilter.add(((ILocationAware)record).getLocation().hashCode());
						SafeChunk chunk = new SafeChunk(((ILocationAware)record).getLocation());
						mSession.ChunkLocationFilter.add(chunk.hashCode());
					}
					
					++index;
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
					
					records.writeAll(dstream, isAbsolute);
			
					// ensure i havent messed up the implementation of getSize()
					Debug.loggedAssert(totalSize == bstream.size(), "Get size returned bad size");
					
					// Work out where to write from and how much padding will be left
					long startLocation = mSession.Location + mSession.TotalSize - mSession.Padding;
					
					long temp = Math.min(mSession.Padding, totalSize);
					mSession.Padding -= temp;
					
					// Write it into the file
					mFile.seek(startLocation);
					Debug.finest("*Writing from %X -> %X", startLocation, startLocation + bstream.size()-1);
					mFile.write(bstream.toByteArray());
		
					// Update the session info
					mSession.EndTimestamp = records.getEndTimestamp();
					mSession.RecordCount += records.size();
					set(indexOf(mSession), mSession);

					CrossReferenceIndex.instance.updateSession(mLog, mSession, records.getAllChunks());
					Debug.info("Completed append to Session %d", mSession.Id);
					
					if(!rolledBackEntries.isEmpty())
						mLog.mRollbackIndex.setRollbackState(mSession, rolledBackEntries, true);
				}
				
				// get data to keep records consistent
				if(!isAbsolute)
				{
					// Apply info to keep it consistent
					Boolean newDepth = records.getCurrentDepth(records.size()-1);
					if(newDepth != null)
						mDeepMode = newDepth;
					
					InventoryRecord newLastInventory = records.getCurrentInventory(records.size()-1);
					if(newLastInventory != null)
						mLastInventory = newLastInventory;
					
					Location newLastLocation = records.getCurrentLocation(records.size()-1);
					if(newLastLocation != null)
						mLastLocation = newLastLocation; 
					
					if(splitSession != null)
					{
						// Apply info to keep it consistent
						SessionInfoRecord info = new SessionInfoRecord(mDeepMode);
						info.setTimestamp(splitSession.getStartTimestamp());
						splitSession.add(0, info);
						
						if(splitSession.getFirstInventory() == null && mLastInventory != null)
						{
							mLastInventory.setTimestamp(splitSession.getStartTimestamp());
							splitSession.add(0, mLastInventory);
						}
						if(splitSession.getFirstLocation() == null && mLastLocation != null)
						{
							TeleportRecord record = new TeleportRecord(mLastLocation, TeleportCause.UNKNOWN);
							record.setTimestamp(splitSession.getStartTimestamp());
							splitSession.add(0,record);
						}
					}
				}
				
				return splitSession;
			}
			finally
			{
				Profiler.endTimingSection();
			}
		}
	}
}
