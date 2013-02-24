package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;


public class RollbackIndex extends Index<RollbackEntry>
{
	private HashMap<Integer,Integer> mRollbackMap = new HashMap<Integer, Integer>();
	
	public RollbackIndex( LogFile log, FileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(log, header, file, locator);
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
	public void add( RollbackEntry entry ) throws IOException
	{
		super.add(entry);
		
		rebuildRollbackMap();
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
		super.read();
		
		rebuildRollbackMap();
	}

	public RollbackEntry getRollbackEntryById(int id)
	{
		if(mRollbackMap.containsKey(id))
			return get(mRollbackMap.get(id));
		
		return null;
	}
}
