package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

public class OwnerTagIndex extends Index<OwnerMapEntry>
{
	private HashMap<Integer,Integer> mTagMap = new HashMap<Integer, Integer>();
	
	public OwnerTagIndex( LogFile log, FileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(log, header, file, locator);
	}

	@Override
	public String getIndexName()
	{
		return "OwnerTag Index";
	}

	@Override
	public long getLocation()
	{
		return mHeader.OwnerMapLocation;
	}

	@Override
	public long getSize()
	{
		return mHeader.OwnerMapSize;
	}

	@Override
	protected int getEntrySize()
	{
		return OwnerMapEntry.cSize;
	}

	@Override
	protected OwnerMapEntry createNewEntry()
	{
		return new OwnerMapEntry();
	}

	@Override
	protected int getElementCount()
	{
		return mHeader.OwnerMapCount;
	}

	@Override
	protected void updateElementCount( int newCount )
	{
		mHeader.OwnerMapCount = newCount;
	}

	@Override
	protected void updateSize( long newSize )
	{
		mHeader.OwnerMapSize = newSize;
	}

	@Override
	protected void updateLocation( long newLocation )
	{
		mHeader.OwnerMapLocation = newLocation;
	}

	private void rebuildTagMap()
	{
		mTagMap.clear();
		
		int index = 0;
		for(OwnerMapEntry entry : mElements)
		{
			mTagMap.put(entry.Id, index);
			++index;
		}
	}
	
	@Override
	public void add( OwnerMapEntry entry ) throws IOException
	{
		super.add(entry);
		
		rebuildTagMap();
	}
	
	@Override
	public void read() throws IOException
	{
		super.read();
		
		rebuildTagMap();
	}
	
	@Override
	public void remove( int index ) throws IOException
	{
		super.remove(index);
		
		rebuildTagMap();
	}
	
	public OwnerMapEntry getOwnerTagById(int id)
	{
		if(mTagMap.containsKey(id))
			return get(mTagMap.get(id));
		
		return null;
	}
}
