package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.structurefile.DataIndex;
import au.com.mineauz.PlayerSpy.structurefile.IMovableData;
import au.com.mineauz.PlayerSpy.structurefile.SpaceLocator;


public class RollbackIndex extends DataIndex<RollbackEntry, IMovableData<RollbackEntry>>
{
	private HashMap<Integer,Integer> mRollbackMap = new HashMap<Integer, Integer>();
	private FileHeader mHeader;
	
	public RollbackIndex( LogFile log, FileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(log, file, locator);
		mHeader = header;
	}

	@Override
	public String getIndexName()
	{
		return "Rollback Index";
	}

	@Override
	public long getLocation()
	{
		return mHeader.RollbackIndexLocation;
	}

	@Override
	public long getSize()
	{
		return mHeader.RollbackIndexSize;
	}

	@Override
	protected int getEntrySize()
	{
		return RollbackEntry.cSize;
	}

	@Override
	protected RollbackEntry createNewEntry()
	{
		return new RollbackEntry();
	}

	@Override
	protected int getElementCount()
	{
		return mHeader.RollbackIndexCount;
	}

	@Override
	protected void updateElementCount( int newCount )
	{
		mHeader.RollbackIndexCount = newCount;
	}

	@Override
	protected void updateSize( long newSize )
	{
		mHeader.RollbackIndexSize = newSize;
	}

	@Override
	protected void updateLocation( long newLocation )
	{
		mHeader.RollbackIndexLocation = newLocation;
	}
	
	private void rebuildRollbackMap()
	{
		mRollbackMap.clear();
		
		int index = 0;
		for(RollbackEntry entry : mElements)
		{
			mRollbackMap.put(entry.sessionId, index);
			++index;
		}
	}
	
	@Override
	public int add( RollbackEntry entry ) throws IOException
	{
		if(mHeader.VersionMajor < 3)
			throw new IllegalStateException("You cannot use the rollback index on a pre Version 3 tracdata file.");
		
		int index = super.add(entry);
		
		rebuildRollbackMap();
		
		return index;
	}
	
	@Override
	public void remove( int index ) throws IOException
	{
		super.remove(index);
		
		rebuildRollbackMap();
	}
	
	@Override
	public void read() throws IOException
	{
		if(mHeader.VersionMajor < 3)
			throw new IllegalStateException("You cannot use the rollback index on a pre Version 3 tracdata file.");
		
		super.read();
		
		rebuildRollbackMap();
	}

	public RollbackEntry getRollbackEntryById(int id)
	{
		if(mRollbackMap.containsKey(id))
			return get(mRollbackMap.get(id));
		
		return null;
	}
	
	@Override
	protected RollbackData getDataFor( RollbackEntry entry )
	{
		return new RollbackData(entry);
	}
	
	public void setRollbackState(SessionEntry session, List<Short> indices, boolean set) throws IOException
	{
		RollbackEntry entry = getRollbackEntryById(session.Id);
		
		if(entry == null)
		{
			entry = new RollbackEntry();
			entry.sessionId = session.Id;
			add(entry);
		}
		
		RollbackData data = getDataFor(entry);
		data.setState(indices, set);
	}
	
	public short[] getRolledBackRecords(SessionEntry session) throws IOException
	{
		RollbackEntry entry = getRollbackEntryById(session.Id);
		if(entry == null)
			return new short[0];
		
		RollbackData data = getDataFor(entry);
		return data.readState();
	}
	
	@Override
	protected void saveChanges() throws IOException
	{
		mFile.seek(0);
		mHeader.write(mFile);
	}
	
	public class RollbackData implements IMovableData<RollbackEntry>
	{
		private final RollbackEntry mRollbackEntry;
		
		public RollbackData(RollbackEntry entry)
		{
			mRollbackEntry = entry;
		}
		
		@Override
		public RollbackEntry getIndexEntry()
		{
			return mRollbackEntry;
		}
		
		public long getSize()
		{
			return mRollbackEntry.detailSize;
		}
		
		@Override
		public long getLocation()
		{
			return mRollbackEntry.detailLocation;
		}
		@Override
		public void setLocation( long newLocation )
		{
			mRollbackEntry.detailLocation = newLocation;
		}
		
		@Override
		public void saveChanges() throws IOException
		{
			set(indexOf(mRollbackEntry), mRollbackEntry);
		}
		
		
		private void write(List<Short> newList) throws IOException
		{
			Debug.fine("Updaing rollback detail for %d", mRollbackEntry.sessionId);
			
			int diff = newList.size() - mRollbackEntry.count;
			long oldSize = mRollbackEntry.count * 2;
			mRollbackEntry.count = (short) newList.size();
			if(diff < 0)
			{
				Debug.finer("Losing %d entries", diff);
				if(newList.size() == 0)
				{
					mLocator.releaseSpace(mRollbackEntry.detailLocation,mRollbackEntry.detailSize);
					remove(mRollbackEntry);
				}
				else
				{
					long newSize = newList.size() * 2;
					
					// Update the entries
					mFile.seek(mRollbackEntry.detailLocation);
					for(Short index : newList)
						mFile.writeShort(index);
					
					mRollbackEntry.padding += (oldSize - newSize);
					
					set(indexOf(mRollbackEntry), mRollbackEntry);
					
					Debug.finer("Adding %d bytes of padding", (oldSize - newSize));
					Debug.logLayout(mHostingFile);
				}
			}
			else if(diff > 0)
			{
				Debug.finer("Gaining %d entries", diff);
				long availableSpace = mRollbackEntry.padding;
				availableSpace += mLocator.getFreeSpace(mRollbackEntry.detailLocation + mRollbackEntry.detailSize);
				
				Debug.finer("*Avaiable Space: " + availableSpace + " padding: " + mRollbackEntry.padding);
				
				long newSize = newList.size() * 2;
				
				if(diff*2 <= availableSpace)
				{
					// Work out how much padding will be left
					newSize = diff*2;
					
					long temp = Math.min(mRollbackEntry.padding, newSize);
					mRollbackEntry.padding -= temp;
					newSize -= temp;
					
					if(mRollbackEntry.padding == 0 && newSize != 0)
					{
						// There is a hole to consume
						mLocator.consumeSpace(mRollbackEntry.detailLocation + mRollbackEntry.detailSize, newSize);
						mRollbackEntry.detailSize += newSize;
					}
					
					mFile.seek(mRollbackEntry.detailLocation);
					for(Short index : newList)
						mFile.writeShort(index);
					
					set(indexOf(mRollbackEntry), mRollbackEntry);
					
					Debug.finest("Rollback detail expanded to %X -> %X", mRollbackEntry.detailLocation, mRollbackEntry.detailLocation + mRollbackEntry.detailSize - 1);
					Debug.logLayout(mHostingFile);
				}
				else
				{
					// Relocate it
					long oldLocation = mRollbackEntry.detailLocation;
					oldSize = mRollbackEntry.detailSize;
					
					// Reset the padding
					mRollbackEntry.padding = 8;
					
					mRollbackEntry.detailLocation = mLocator.findFreeSpace(newSize);
					mLocator.consumeSpace(mRollbackEntry.detailLocation, newSize);
					
					mRollbackEntry.detailSize = newSize;
					
					mFile.seek(mRollbackEntry.detailLocation);
					for(Short index : newList)
						mFile.writeShort(index);
					
					// Update rollback entry
					set(indexOf(mRollbackEntry), mRollbackEntry);
					
					Debug.finest("Rollback detail reloated to %X -> %X from %X -> %X", mRollbackEntry.detailLocation, mRollbackEntry.detailLocation + mRollbackEntry.detailSize - 1, oldLocation, oldLocation + oldSize - 1);
					Debug.logLayout(mHostingFile);
					
					mLocator.releaseSpace(oldLocation, oldSize);
					
					
				}
			}
		}
		
		public void setState(List<Short> indices, boolean set) throws IOException
		{
			short[] existing = readState();
			
			// Modify the detail
			boolean[] add = new boolean[indices.size()];
			boolean[] remove = new boolean[indices.size()];
			
			for(int ind = 0; ind < indices.size(); ++ind)
			{
				add[ind] = true;
				remove[ind] = false;
				for(int i = 0; i < existing.length; ++i)
				{
					if(existing[i] == (short)indices.get(ind))
					{
						add[ind] = false;
						if(!set)
						{
							// Remove this item
							remove[ind] = true;
						}
						break;
					}
				}
			}
			
			ArrayList<Short> newList = new ArrayList<Short>(existing.length);
			for(int i = 0; i < existing.length; ++i)
				newList.add(existing[i]);
			
			for(int i = 0; i < indices.size(); ++i)
			{
				if(remove[i])
					newList.remove(indices.get(i));
				if(add[i])
					newList.add(indices.get(i));
			}
			
			write(newList);
		}
		
		public short[] readState() throws IOException
		{
			mFile.seek(mRollbackEntry.detailLocation);
			short[] data = new short[mRollbackEntry.count];
			for(int i = 0; i < mRollbackEntry.count; ++i)
				data[i] = mFile.readShort();
			
			return data;
		}
		
		@Override
		public String toString()
		{
			return String.format("RBData {session: %d}", mRollbackEntry.sessionId);
		}
	}

}
