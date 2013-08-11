package au.com.mineauz.PlayerSpy.structurefile;

import java.io.IOException;

import au.com.mineauz.PlayerSpy.debugging.Debug;

public class SpaceLocator
{
	private AbstractHoleIndex mIndex;
	private StructuredFile mHostingFile;
	public SpaceLocator(StructuredFile structureFile)
	{
		mHostingFile = structureFile;
	}
	
	public void setHoleIndex(AbstractHoleIndex index)
	{
		mIndex = index;
	}
	
	private int findHoleAt(long location)
	{
		int i = 0;
		for(HoleEntry hole : mIndex)
		{
			if(hole.Location <= location && hole.Location + hole.Size > location)
				return i;
			else if(hole.Location > location)
				return -1;
			
			i++;
		}
		
		return -1;
	}
	
	public long getFreeSpace(long location)
	{
		int hole = findHoleAt(location);
		
		if(hole != -1)
		{
			HoleEntry entry = mIndex.get(hole);
			mHostingFile.checkSpaceStatus(entry.Location, entry.Size, true);
			return entry.Size;
		}
		
		if(location == mIndex.getEndOfFile())
			return Long.MAX_VALUE;

		return 0;
	}
	public boolean isFreeSpace(long location, long size)
	{
		return getFreeSpace(location) >= size;
	}
	
	public long findFreeSpace(long size)
	{
		for(HoleEntry hole : mIndex)
		{
			if(hole.Size >= size)
			{
				mHostingFile.checkSpaceStatus(hole.Location, hole.Size, true);
				
				Debug.finest("Found free space at %X->%X", hole.Location, hole.Location + hole.Size - 1);
				return hole.Location;
			}
		}
		
		Debug.finest("Found free space at EOF (%X)", mIndex.getEndOfFile());
		return mIndex.getEndOfFile();
	}
	
	public void consumeSpace(long location, long size) throws IOException
	{
		int hole = findHoleAt(location);
		
		if(hole == -1 && location != mIndex.getEndOfFile())
		{
			Debug.recordVariable("Spacelocator Location", location);
			Debug.recordVariable("Spacelocator Size", size);
			throw new RuntimeException("The location asked for did not have a hole to consume");
		}
		
		if(hole != -1)
		{
			HoleEntry entry = mIndex.get(hole);
			
			if(location == entry.Location)
			{
				if(location + size >= entry.Location + entry.Size)
				{
					// It completely covers the hole
					mIndex.remove(hole);
				}
				else
				{
					// It covers the start of the hole
					entry.Size = (entry.Location + entry.Size) - (location + size);
					entry.Location += size;
					
					if(entry.Size > 0)
						mIndex.set(hole, entry);
					else
						mIndex.remove(hole);
				}
			}
			else
			{
				if(location + size >= entry.Location + entry.Size)
				{
					// It covers the end of the hole
					entry.Size = (location - entry.Location);
					
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
			
			Debug.finest("Consumed space %X-%X", location, location + size - 1);
			Debug.logLayout(mHostingFile);
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
