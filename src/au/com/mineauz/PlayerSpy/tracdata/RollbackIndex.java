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
	public int add( RollbackEntry entry ) throws IOException
	{
		if(mHeader.VersionMajor < 3)
			throw new IllegalStateException("You cannot use the rollback index on a pre Version 3 tracdata file.");
		
		// Rollback indices are only present in Version 3.1 logs or higher
		
		if(mHeader.VersionMajor == 3 && mHeader.VersionMinor < 1)
		{
			mHeader.VersionMinor = 1;
			mFile.seek(0);
			mHeader.write(mFile);
		}
		
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
		
		if(mHeader.VersionMajor == 3 && mHeader.VersionMinor < 1)
			// It is not present in version 3.0
			return;
		
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
