package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;

public class SpaceLocator
{
	private final HoleIndex mIndex;
	public SpaceLocator(HoleIndex index)
	{
		mIndex = index;
	}
	
	private int findHoleAt(long location, Object key)
	{
		int i = 0;
		for(HoleEntry hole : mIndex)
		{
			if(hole.Location <= location && hole.Location + hole.Size >= location && (hole.AttachedTo == null || hole.AttachedTo.equals(key)))
				return i;
			else if(hole.Location + hole.Size > location)
				return -1;
			
			i++;
		}
		
		return -1;
	}
	
	public boolean isFreeSpace(long location, long size)
	{
		return isFreeSpace(location, size, null);
	}
	
	public boolean isFreeSpace(long location, long size, Object key)
	{
		int hole = findHoleAt(location, key);
		
		if(hole != -1)
		{
			HoleEntry entry = mIndex.get(hole);
			
			if(entry.Size - (location - entry.Location) >= size)
				return true;
		}
		
		if(location == mIndex.getEndOfFile())
			return true;

		return false;
	}
	
	public long findFreeSpace(long size)
	{
		return findFreeSpace(size, null);
	}
	
	public long findFreeSpace(long size, Object key)
	{
		for(HoleEntry hole : mIndex)
		{
			if(hole.AttachedTo != null && !hole.AttachedTo.equals(key))
				continue;
				
			if(hole.Size >= size)
				return hole.Location;
		}
		
		return mIndex.getEndOfFile();
	}
	
	public void consumeSpace(long location, long size) throws IOException
	{
		consumeSpace(location, size, null);
	}
	
	public void consumeSpace(long location, long size, Object key) throws IOException
	{
		int hole = findHoleAt(location, key);
		
		if(hole != -1)
		{
			HoleEntry entry = mIndex.get(hole);
			
			if(location == entry.Location)
			{
				if(size == entry.Size)
				{
					// It completely covers the hole
					mIndex.remove(hole);
				}
				else
				{
					// It covers the start of the hole
					entry.Size = (entry.Location + entry.Size) - (location + size);
					entry.Location += size;
					mIndex.set(hole, entry);
				}
			}
			else
			{
				if(location + size == entry.Location + entry.Size)
				{
					// It covers the end of the hole
					entry.Size = (location - entry.Location);
					entry.Location = location;
					
					mIndex.set(hole, entry);
				}
				else
				{
					// It covers the middle of the hole
					HoleEntry newHole = new HoleEntry();
					newHole.Location = location + size;
					newHole.Size = (entry.Location + entry.Size) - (location + size);
					
					entry.Size = (location - entry.Location);
					
					mIndex.set(hole, entry);
					mIndex.add(newHole);
				}
			}
		}
	}
	
	public void releaseSpace(long location, long size) throws IOException
	{
		HoleEntry hole = new HoleEntry();
		hole.Location = location;
		hole.Size = size;
		
		mIndex.add(hole);
	}
}
