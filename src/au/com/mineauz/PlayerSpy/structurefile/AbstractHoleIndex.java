package au.com.mineauz.PlayerSpy.structurefile;

import java.io.IOException;
import java.io.RandomAccessFile;

import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.structurefile.DataIndex;
import au.com.mineauz.PlayerSpy.structurefile.IMovableData;
import au.com.mineauz.PlayerSpy.structurefile.SpaceLocator;

public abstract class AbstractHoleIndex extends DataIndex<HoleEntry, IMovableData<HoleEntry>>
{

	public AbstractHoleIndex( StructuredFile hostingFile, RandomAccessFile file, SpaceLocator locator )
	{
		super(hostingFile, file, locator);
	}

	@Override
	public String getIndexName()
	{
		return "Hole Index";
	}

	@Override
	public abstract long getLocation();

	@Override
	public abstract long getSize();

	@Override
	protected int getEntrySize()
	{
		return HoleEntry.cSize;
	}

	@Override
	protected HoleEntry createNewEntry()
	{
		return new HoleEntry();
	}

	@Override
	protected abstract int getElementCount();

	@Override
	protected abstract void updateElementCount( int newCount );

	@Override
	protected abstract void updateSize( long newSize );

	@Override
	protected abstract void updateLocation( long newLocation );
	
	protected abstract int getPadding();
	
	protected abstract void updatePadding( int newPadding );
	
	long getEndOfFile()
	{
		try
		{
			return mFile.length();
		}
		catch(IOException e)
		{
			return 0;
		}
	}

	private boolean canMergeHoles(HoleEntry a, HoleEntry b)
	{
		if((a.Location >= b.Location && a.Location <= b.Location + b.Size) ||
		   (a.Location + a.Size >= b.Location && a.Location + a.Size <= b.Location + b.Size))
			return true;
		
		return false;
	}
	private HoleEntry mergeHoles(HoleEntry a, HoleEntry b)
	{
		if(!canMergeHoles(a,b))
			return null;
		
		HoleEntry merged = new HoleEntry();
		if(a.Location < b.Location)
		{
			merged.Location = a.Location;
			merged.Size = Math.max((b.Location - a.Location) + b.Size, a.Size);
		}
		else
		{
			merged.Location = b.Location;
			merged.Size = Math.max(b.Size, (a.Location - b.Location) + a.Size);
		}
		
		return merged;
	}
	
	@Override
	protected int getInsertIndex( HoleEntry entry )
	{
		int index = 0;
		for(index = 0; index < mElements.size(); index++)
		{
			if(mElements.get(index).Location > entry.Location)
				break;
		}
		
		return index;
	}
	
	// The holes index uses padding so it needs to do these methods custom
	
	@Override
	public int add( HoleEntry entry ) throws IOException
	{
		if(entry.Size == 0)
			return -1;
		
		Debug.finer("Adding hole from %X->%X", entry.Location, entry.Location + entry.Size - 1);
		
		// Prevent holes from being added at eof
		if(entry.Location + entry.Size == getEndOfFile())
		{
			mFile.setLength(entry.Location);
			Debug.finer("Hole was at EOF, shrinking file to %X", getEndOfFile());
			Debug.logLayout(mHostingFile);
			
			return mElements.size();
		}
		
		// Check if we need to merge it
		boolean merged = false;
		int mergeIndex = 0;
		for(int i = 0; i < mElements.size(); i++)
		{
			HoleEntry existing = mElements.get(i);
			if(canMergeHoles(entry,existing))
			{
				mElements.set(i, mergeHoles(entry,existing));
				entry = mElements.get(i);
				Debug.finest("Merging new hole into @%d changing range from (%X->%X) into (%X->%X)", i, existing.Location, existing.Location + existing.Size-1,entry.Location, entry.Location + entry.Size-1);
				
				if(merged)
					remove(i);
				else
				{
					// Write the changes
					mFile.seek(getLocation() + i * getEntrySize());
					entry.write(mFile);
				}
								
				merged = true;
				mergeIndex = i;
			}
		}
		
		if(merged)
		{
			Debug.logLayout(mHostingFile);
			return mergeIndex;
		}
		
		int insertIndex = getInsertIndex(entry);
		mElements.add(insertIndex, entry);
		
		// Use padding first
		if(getPadding() >= getEntrySize())
		{
			// Shift the entries
			write(insertIndex);
			
			updateElementCount(mElements.size());
			updatePadding(getPadding() - getEntrySize());
			updateSize(mElements.size() * getEntrySize());
			
			Debug.finest("Wrote %d/%d hole entries to %X -> %X Using padding. Remaining: %d bytes", mElements.size() - insertIndex, mElements.size(), getLocation() + insertIndex * getEntrySize(), getLocation() + getSize()-1, getPadding());
			Debug.logLayout(mHostingFile);
		}
		// Use a hole
		else if(mLocator.isFreeSpace(getLocation() + getSize(), getEntrySize()))
		{
			updateElementCount(mElements.size());
			updateSize(mElements.size() * getEntrySize());
			
			mLocator.consumeSpace(getLocation() + getSize() - getEntrySize(), getEntrySize());
			
			
			write(insertIndex);

			Debug.finest("Wrote %d/%d hole entries to %X -> %X", mElements.size() - insertIndex, mElements.size(), getLocation() + insertIndex * getEntrySize(), getLocation() + getSize()-1);
			Debug.logLayout(mHostingFile);
		}
		// Relocate it
		else
		{
			long oldLocation = getLocation();
			long oldSize = getSize();
			
			HoleEntry oldIndexHole = new HoleEntry();
			oldIndexHole.Location = oldLocation;
			oldIndexHole.Size = oldSize;
			
			// Try to merge the old hole
			merged = false;
			for(int i = 0; i < mElements.size(); i++)
			{
				if(canMergeHoles(oldIndexHole,mElements.get(i)))
				{
					mElements.set(i, mergeHoles(oldIndexHole,mElements.get(i)));
					Debug.finest("Merged hole for old location into @%d. Range is now %X -> %X", i, mElements.get(i).Location, mElements.get(i).Location + mElements.get(i).Size - 1);
					
					oldIndexHole = mElements.get(i);
					
					if(merged)
						mElements.remove(oldIndexHole);
					
					merged = true;
				}
			}
			
			// Add it now if it wasnt merged
			if(!merged && oldIndexHole.Size != 0)
			{
				mElements.add(getInsertIndex(oldIndexHole), oldIndexHole);
				Debug.finest("Adding hole for old location %X -> %X", oldLocation, oldLocation + oldSize - 1);
			}
			
			updatePadding(HoleEntry.cSize);
			// The total size of the hole index now including 1 extra entry as padding
			long newSize = mElements.size() * getEntrySize() + getPadding();
			
			// Temporary relocate so that the hole index can be placed into a space that used to include this index
			updateLocation(mFile.length());
			
			// Find a new location for the index
			long newLocation = mLocator.findFreeSpace(newSize);
			
			int elCount = mElements.size();
			// ConsumeSpace. This is the same as in space locator but it makes sure there are no writes to file, just updates the list
			for(HoleEntry hole : mElements)
			{
				if(hole.Location != newLocation)
					continue;
				
				if(newLocation == hole.Location)
				{
					if(newLocation + newSize >= hole.Location + hole.Size)
					{
						// It completely covers the hole
						mElements.remove(hole);
					}
					else
					{
						// It covers the start of the hole
						hole.Size = (hole.Location + hole.Size) - (newLocation + newSize);
						hole.Location += newSize;
						
						if(hole.Size <= 0)
							mElements.remove(hole);
					}
				}
				
				Debug.finest("Consumed space %X-%X", newLocation, newLocation + newSize - 1);
				Debug.logLayout(mHostingFile);
				break;
			}
			
			if(elCount > mElements.size())
			{
				// Something was removed, add it to padding
				updatePadding(getPadding() + (HoleEntry.cSize * (elCount - mElements.size())));
			}
			
			// Prepare the header info
			updateElementCount(mElements.size());
			updateSize(newSize);
			updateLocation(newLocation);
			
			// Write the items
			write(0);
			
			// Padding
			mFile.write(new byte[getPadding()]);

			Debug.finest("Moving hole index from %X to (%X -> %X) setting %d bytes padding", oldLocation, getLocation(), getLocation() + getSize() - 1, getPadding());
			Debug.logLayout(mHostingFile);
		}
		
		saveChanges();
		
		return insertIndex;
	}
	
	@Override
	public void remove( int index ) throws IOException
	{
		mElements.remove(index);
		
		write(index);
		
		updatePadding(getPadding() + getEntrySize());
		//updateSize(mElements.size() * getEntrySize() + getPadding());
		updateElementCount(mElements.size());
		
		Debug.finer("Entry %d removed from %s", index, getIndexName());
		Debug.logLayout(mHostingFile);
		
		saveChanges();
	}
	
	public HoleEntry getHoleAfter(long location)
	{
		for(HoleEntry entry : mElements)
		{
			if(entry.Location >= location)
				return entry;
		}
		
		return null;
	}
	
	@Override
	public void set( int index, HoleEntry entry ) throws IOException
	{
		// Try a merge
		for(int i = 0; i < mElements.size(); i++)
		{
			if(i == index)
				continue;
			
			HoleEntry existing = mElements.get(i);
			if(canMergeHoles(entry,existing))
			{
				HoleEntry newHole = mergeHoles(entry,existing);
				mElements.set(i, newHole);
				
				// Write the changes
				mFile.seek(getLocation() + i * getEntrySize());
				newHole.write(mFile);
				
				Debug.finest("Merging new hole into @%d changing range from (%X->%X) into (%X->%X)", i, existing.Location, existing.Location + existing.Size-1,newHole.Location, newHole.Location + newHole.Size-1);
				Debug.logLayout(mHostingFile);
				return;
			}
		}
		
		// Cant merge, just set it
		super.set(index, entry);

		Debug.fine("Set hole @%d range to %X->%X", index, entry.Location, entry.Location + entry.Size - 1);
		Debug.logLayout(mHostingFile);
	}

	@Override
	protected HoleData getDataFor( HoleEntry entry )
	{
		return new HoleData(entry);
	}
	
	public class HoleData implements IMovableData<HoleEntry>
	{
		private final HoleEntry mHole;
		
		public HoleData(HoleEntry hole)
		{
			mHole = hole;
		}
		
		@Override
		public HoleEntry getIndexEntry()
		{
			return mHole;
		}
		
		@Override
		public long getLocation()
		{
			return mHole.Location;
		}
		@Override
		public void setLocation( long newLocation )
		{
			mHole.Location = newLocation;
		}

		@Override
		public long getSize()
		{
			return mHole.Size;
		}

		@Override
		public void saveChanges() throws IOException
		{
			set(indexOf(mHole),mHole);
		}

	}

}
