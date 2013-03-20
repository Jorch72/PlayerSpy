package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import au.com.mineauz.PlayerSpy.structurefile.Index;
import au.com.mineauz.PlayerSpy.structurefile.SpaceLocator;

public class OwnerTagIndex extends Index<OwnerMapEntry>
{
	private HashMap<Integer,Integer> mTagMap = new HashMap<Integer, Integer>();
	private FileHeader mHeader;
	
	public OwnerTagIndex( LogFile log, FileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(log, file, locator);
		mHeader = header;
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
	public int add( OwnerMapEntry entry ) throws IOException
	{
		if(mHeader.VersionMajor < 2)
			throw new IllegalStateException("You cannot use the ownertag index on a pre Version 2 tracdata file.");
		
		int index = super.add(entry);
		
		rebuildTagMap();
		
		return index;
	}
	
	@Override
	public void read() throws IOException
	{
		if(mHeader.VersionMajor < 2)
			throw new IllegalStateException("You cannot use the ownertag index on a pre Version 2 tracdata file.");
		
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
	
	public int getOrCreateTag(String owner) throws IOException
	{
		// See if there is a tag we can reuse
		for(OwnerMapEntry tag : this)
		{
			if(tag.Owner.equalsIgnoreCase(owner))
			{
				return tag.Id;
			}
		}
		
		int id = SessionIndex.NextId++;
			
		// Add a new tag
		OwnerMapEntry ent = new OwnerMapEntry();
		ent.Owner = owner;
		ent.Id = id;
		
		add(ent);
		
		return id;
	}
	
	@Override
	protected void saveChanges() throws IOException
	{
		mFile.seek(0);
		mHeader.write(mFile);
	}
}
