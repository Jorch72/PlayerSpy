package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.Validate;

import au.com.mineauz.PlayerSpy.Utilities.CubicChunk;
import au.com.mineauz.PlayerSpy.structurefile.DataIndex;
import au.com.mineauz.PlayerSpy.structurefile.IMovableData;
import au.com.mineauz.PlayerSpy.structurefile.SpaceLocator;
import au.com.mineauz.PlayerSpy.structurefile.StructuredFile;

public class ChunkIndex extends DataIndex<ChunkEntry, IMovableData<ChunkEntry>>
{
	private FileHeader mHeader;
	
	private HashMap<Integer, Integer> mIdMap = new HashMap<Integer, Integer>();
	
	public ChunkIndex( StructuredFile hostingFile, FileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(hostingFile, file, locator);
		mHeader = header;
	}
	@Override
	protected ChunkList getDataFor( ChunkEntry entry )
	{
		return new ChunkList(entry);
	}
	@Override
	public String getIndexName()
	{
		return "Chunk List Index";
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
			mIdMap.put(entry.listId, index);
			++index;
		}
	}
	
	@Override
	public int add( ChunkEntry entry ) throws IOException
	{
		int result = super.add(entry);
		
		rebuildMap();
		
		return result;
	}
	
	@Override
	public void read() throws IOException
	{
		if(mHeader.VersionMajor < 4)
			throw new IllegalStateException("You cannot use the ownertag index on a pre Version 2 tracdata file.");
		
		super.read();
		
		rebuildMap();
	}
	
	@Override
	public void remove( int index ) throws IOException
	{
		super.remove(index);
		
		rebuildMap();
	}
	
	public int createNewChunkList(Set<CubicChunk> chunks) throws IOException
	{
		ChunkEntry entry = createNewEntry();
		entry.listId = SessionIndex.NextId++;
		add(entry);
		
		ChunkList list = getDataFor(entry);
		list.addChunks(chunks);
		
		return entry.listId;
	}
	
	public void appendChunks(int chunkListId, Set<CubicChunk> chunks) throws IOException
	{
		Integer index = mIdMap.get(chunkListId);
		
		Validate.notNull(index);
			
		ChunkEntry entry = mElements.get(index);
		ChunkList list = getDataFor(entry);
		list.addChunks(chunks);
	}
	
	public Set<CubicChunk> getChunks(int listId) throws IOException
	{
		Integer index = mIdMap.get(listId);
		
		if(index == null)
			return null;
		
		ChunkEntry entry = mElements.get(index);
		ChunkList list = getDataFor(entry);
		
		return list.getChunks();
	}
	
	public class ChunkList implements IMovableData<ChunkEntry>
	{
		private ChunkEntry mEntry;
		
		public ChunkList(ChunkEntry entry)
		{
			mEntry = entry;
		}
		
		@Override
		public long getLocation()
		{
			return mEntry.location;
		}

		@Override
		public long getSize()
		{
			return mEntry.size;
		}

		@Override
		public ChunkEntry getIndexEntry()
		{
			return mEntry;
		}

		@Override
		public void setLocation( long newLocation )
		{
			mEntry.location = newLocation;
		}

		@Override
		public void saveChanges() throws IOException
		{
			set(indexOf(mEntry), mEntry);
		}
		
		private void writeEntries(Set<CubicChunk> chunks) throws IOException
		{
			for(CubicChunk chunk : chunks)
				chunk.write(mFile);
		}
		
		private void write(Set<CubicChunk> chunks) throws IOException
		{
			int newSize = CubicChunk.getSize() * chunks.size();
			
			long availableSpace = mLocator.getFreeSpace(mEntry.location + mEntry.size);
			
			if(newSize - getSize() < availableSpace) // Todo: newSize - getSize()
			{
				// There is a hole to consume
				mLocator.consumeSpace(mEntry.location + mEntry.size, newSize - getSize());
				mEntry.size = newSize;
				
				mFile.seek(mEntry.location);
				writeEntries(chunks);
				
				mEntry.count = chunks.size();
				set(indexOf(mEntry), mEntry);
			}
			else
			{
				// Relocate it
				long oldLocation = mEntry.location;
				long oldSize = mEntry.size;
				
				mEntry.location = mLocator.findFreeSpace(newSize);
				mLocator.consumeSpace(mEntry.location, newSize);
				
				mEntry.size = newSize;
				
				mFile.seek(mEntry.location);
				writeEntries(chunks);
				
				mEntry.count = chunks.size();
				
				// Update entry
				set(indexOf(mEntry), mEntry);
				
				mLocator.releaseSpace(oldLocation, oldSize);
			}
		}
				
		
		public Set<CubicChunk> getChunks() throws IOException
		{
			mFile.seek(mEntry.location);
			
			HashSet<CubicChunk> chunks = new HashSet<CubicChunk>();
			
			for(int i = 0; i < mEntry.count; ++i)
			{
				CubicChunk chunk = new CubicChunk();
				chunk.read(mFile);
				chunks.add(chunk);
			}
			
			return chunks;
		}
		
		public void addChunks(Set<CubicChunk> chunks) throws IOException
		{
			Set<CubicChunk> present = getChunks();
			present.addAll(chunks);
			
			write(present);
		}
	}
}
