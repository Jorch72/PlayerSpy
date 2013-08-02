package au.com.mineauz.PlayerSpy.globalreference;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.structurefile.DataIndex;
import au.com.mineauz.PlayerSpy.structurefile.IMovableData;
import au.com.mineauz.PlayerSpy.structurefile.SpaceLocator;
import au.com.mineauz.PlayerSpy.structurefile.StructuredFile;

public class ChunkIndex extends DataIndex<ChunkEntry, IMovableData<ChunkEntry>>
{
	private GRFileHeader mHeader;
	private Multimap<Long, Integer> mIdMap = ArrayListMultimap.create();
	
	public ChunkIndex( StructuredFile hostingFile, GRFileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(hostingFile, file, locator);
		mHeader = header;
	}

	@Override
	protected ChunkData getDataFor( ChunkEntry entry )
	{
		return new ChunkData(entry);
	}

	@Override
	public String getIndexName()
	{
		return "Chunk Index";
	}

	@Override
	public long getLocation()
	{
		return mHeader.ChunkIndexLocation;
	}

	@Override
	public long getSize()
	{
		return mHeader.ChunkIndexSize;
	}

	@Override
	protected int getEntrySize()
	{
		return ChunkEntry.getEntrySize();
	}

	@Override
	protected ChunkEntry createNewEntry()
	{
		return new ChunkEntry();
	}

	@Override
	protected int getElementCount()
	{
		return mHeader.ChunkIndexCount;
	}

	@Override
	protected void updateElementCount( int newCount )
	{
		mHeader.ChunkIndexCount = newCount;
	}

	@Override
	protected void updateSize( long newSize )
	{
		mHeader.ChunkIndexSize = newSize;
	}

	@Override
	protected void updateLocation( long newLocation )
	{
		mHeader.ChunkIndexLocation = newLocation;
	}

	@Override
	protected void saveChanges() throws IOException
	{
		mFile.seek(0);
		mHeader.write(mFile);
	}
	
	private void rebuildMap()
	{
		mIdMap.clear();
		
		int index = 0;
		for(ChunkEntry entry : mElements)
		{
			mIdMap.put(Utility.getChunkId(entry.chunkX, entry.chunkZ), index);
			++index;
		}
	}
	
	@Override
	public int add( ChunkEntry entry ) throws IOException
	{
		int id = super.add(entry);
		rebuildMap();
		
		return id;
	}
	
	@Override
	public void remove( int index ) throws IOException
	{
		super.remove(index);
		
		rebuildMap();
	}
	
	@Override
	public void read() throws IOException
	{
		super.read();
		
		rebuildMap();
	}
	
	public Multimap<UUID, Integer> getSessionsInChunk(int chunkX, int chunkZ, int worldHash) throws IOException
	{
		Collection<Integer> indexes = mIdMap.get(Utility.getChunkId(chunkX, chunkZ));
		
		for(int index : indexes)
		{
			ChunkEntry entry = mElements.get(index);
			if(entry.worldHash == worldHash)
			{
				ChunkData data = getDataFor(entry);
				
				return data.getContainedSessions();
			}
		}
		
		return null;
	}
	
	public void addSessionToChunk(int chunkX, int chunkZ, int worldHash, UUID fileId, int sessionId) throws IOException
	{
		Collection<Integer> indexes = mIdMap.get(Utility.getChunkId(chunkX, chunkZ));
		
		for(int index : indexes)
		{
			ChunkEntry entry = mElements.get(index);
			if(entry.worldHash == worldHash)
			{
				ChunkData data = getDataFor(entry);
				data.addContainedSession(fileId, sessionId);
				return;
			}
		}
		
		// No existing list
		ChunkEntry entry = new ChunkEntry();
		entry.chunkX = chunkX;
		entry.chunkZ = chunkZ;
		entry.count = 0;
		entry.worldHash = worldHash;
		
		add(entry);
		
		ChunkData data = getDataFor(entry);
		data.addContainedSession(fileId, sessionId);
	}
	
	public void removeSessionFromChunk(int chunkX, int chunkZ, int worldHash, UUID fileId, int sessionId) throws IOException
	{
		Collection<Integer> indexes = mIdMap.get(Utility.getChunkId(chunkX, chunkZ));
		
		for(int index : indexes)
		{
			ChunkEntry entry = mElements.get(index);
			if(entry.worldHash == worldHash)
			{
				ChunkData data = getDataFor(entry);
				data.removeContainedSession(fileId, sessionId);
				return;
			}
		}
	}
	
	public class ChunkData implements IMovableData<ChunkEntry>
	{
		private final ChunkEntry mChunkEntry;
		public ChunkData(ChunkEntry entry)
		{
			mChunkEntry = entry;
		}
		@Override
		public long getLocation()
		{
			return mChunkEntry.location;
		}
		@Override
		public long getSize()
		{
			return mChunkEntry.size;
		}
		@Override
		public ChunkEntry getIndexEntry()
		{
			return mChunkEntry;
		}
		@Override
		public void setLocation( long newLocation )
		{
			mChunkEntry.location = newLocation;
		}
		@Override
		public void saveChanges() throws IOException
		{
			set(indexOf(mChunkEntry), mChunkEntry);
		}
		
		private void writeEntries(Collection<Entry<UUID, Integer>> sessions) throws IOException
		{
			for(Entry<UUID, Integer> session : sessions)
			{
				mFile.writeLong(session.getKey().getMostSignificantBits());
				mFile.writeLong(session.getKey().getLeastSignificantBits());
				
				mFile.writeInt(session.getValue());
			}
		}
		
		private void write(Multimap<UUID, Integer> sessions) throws IOException
		{
			Debug.fine("Updaing chunk list %s", mChunkEntry);
			int newSize = 20 * sessions.entries().size();
			long oldSize = getSize() - mChunkEntry.padding;
			
			if(newSize < oldSize)
			{
				Debug.finer("Losing %d bytes", oldSize - newSize);
				if(newSize == 0)
				{
					mLocator.releaseSpace(getLocation(), getSize());
					remove(mChunkEntry);
				}
				else
				{
					mFile.seek(getLocation());
					writeEntries(sessions.entries());
					
					mChunkEntry.padding += (oldSize - newSize);
					mChunkEntry.count = sessions.entries().size();
					
					set(indexOf(mChunkEntry), mChunkEntry);
					
					Debug.finer("Adding %d bytes of padding", (oldSize - newSize));
					Debug.logLayout(mHostingFile);
				}
			}
			else
			{
				Debug.finer("Gaining %d bytes", newSize - oldSize);
				long availableSpace = mChunkEntry.padding;
				availableSpace += mLocator.getFreeSpace(mChunkEntry.location + mChunkEntry.size);
				
				Debug.finer("*Avaiable Space: " + availableSpace + " padding: " + mChunkEntry.padding);
				
				if(newSize - oldSize < availableSpace)
				{
					long adding = newSize - oldSize;
					long temp = Math.min(mChunkEntry.padding, adding);
					mChunkEntry.padding -= temp;
					adding -= temp;
					
					if(mChunkEntry.padding == 0 && adding != 0)
					{
						// There is a hole to consume
						mLocator.consumeSpace(mChunkEntry.location + mChunkEntry.size, adding);
						mChunkEntry.size += adding;
					}
					
					mFile.seek(mChunkEntry.location);
					writeEntries(sessions.entries());
					
					mChunkEntry.count = sessions.entries().size();
					set(indexOf(mChunkEntry), mChunkEntry);
					
					Debug.finest("Chunklist expanded to %X -> %X", mChunkEntry.location, mChunkEntry.location + mChunkEntry.size - 1);
					Debug.logLayout(mHostingFile);
				}
				else
				{
					// Relocate it
					long oldLocation = mChunkEntry.location;
					oldSize = mChunkEntry.size;
					
					// Reset the padding
					mChunkEntry.padding = 8;
					
					mChunkEntry.location = mLocator.findFreeSpace(newSize);
					mLocator.consumeSpace(mChunkEntry.location, newSize);
					
					mChunkEntry.size = newSize;
					
					mFile.seek(mChunkEntry.location);
					writeEntries(sessions.entries());
					
					mChunkEntry.count = sessions.entries().size();
					
					// Update entry
					set(indexOf(mChunkEntry), mChunkEntry);
					
					Debug.finest("Chunklist reloated to %X -> %X from %X -> %X", mChunkEntry.location, mChunkEntry.location + mChunkEntry.size - 1, oldLocation, oldLocation + oldSize - 1);
					Debug.logLayout(mHostingFile);
					
					mLocator.releaseSpace(oldLocation, oldSize);
				}
			}
		}
		
		public Multimap<UUID, Integer> getContainedSessions() throws IOException
		{
			ArrayListMultimap<UUID, Integer> sessions = ArrayListMultimap.create();
			
			mFile.seek(mChunkEntry.location);
			
			for(int i = 0; i < mChunkEntry.count; ++i)
			{
				UUID fileId = new UUID(mFile.readLong(), mFile.readLong());
				int sessionId = mFile.readInt();
				
				sessions.put(fileId, sessionId);
			}
			
			return sessions;
		}
		
		public void addContainedSession(UUID fileId, Integer sessionId) throws IOException
		{
			Multimap<UUID, Integer> sessions = getContainedSessions();
			
			if(sessions.containsEntry(fileId, sessionId))
				return;
			
			if(sessions.put(fileId, sessionId))
				write(sessions);
		}
		
		public void removeContainedSession(UUID fileId, Integer sessionId) throws IOException
		{
			Multimap<UUID, Integer> sessions = getContainedSessions();
			
			if(sessions.remove(fileId, sessionId))
				write(sessions);
		}
	}
}
