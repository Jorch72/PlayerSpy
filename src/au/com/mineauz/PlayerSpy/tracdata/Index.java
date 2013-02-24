package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.UnmodifiableIterator;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.debugging.Debug;

public abstract class Index<T extends IndexEntry> implements Iterable<T>
{
	protected final FileHeader mHeader;
	protected final RandomAccessFile mFile;
	protected final SpaceLocator mLocator;
	protected final ArrayList<T> mElements = new ArrayList<T>();
	protected final LogFile mLog;
	
	public Index(LogFile log, FileHeader header, RandomAccessFile file, SpaceLocator locator)
	{
		mLog = log;
		mHeader = header;
		mFile = file;
		mLocator = locator;
	}

	/**
	 * Gets the name of this index for debugging
	 */
	public abstract String getIndexName();
	
	/**
	 * Gets the location of this index in the file
	 */
	public abstract long getLocation();

	/**
	 * Gets the total size of this index in the file
	 */
	public abstract long getSize();

	/**
	 * Gets the size of 1 element in this index
	 */
	protected abstract int getEntrySize();
	
	/**
	 * Called when a new IndexEntry element is needed.
	 */
	protected abstract T createNewEntry();

	/**
	 * Gets how many elements are stored on disk
	 */
	protected abstract int getElementCount();
	
	/**
	 * Updates the new element count on the disk
	 * Dont save it to disk
	 */
	protected abstract void updateElementCount(int newCount);
	
	/**
	 * Updates the new size on the disk
	 * Dont save it to disk
	 */
	protected abstract void updateSize(long newSize);
	
	/**
	 * Updates the new location on the disk
	 * Dont save it to disk
	 */
	protected abstract void updateLocation(long newLocation);
	
	/**
	 * Gets where in the list it will be inserted 
	 */
	protected int getInsertIndex(T entry)
	{
		return mElements.size();
	}
	
	/**
	 * Called before the entry is removed
	 */
	protected void onRemove(T entry) {}
	
	public void read() throws IOException
	{
		mElements.clear();
		
		mFile.seek(getLocation());
		
		for(int i = 0; i < getElementCount(); ++i)
		{
			T entry = createNewEntry();
			entry.read(mFile);
			mElements.add(entry);
		}
	}
	
	protected void write(int startIndex) throws IOException
	{
		mFile.seek(getLocation() + startIndex * getEntrySize());
		for(int i = startIndex; i < mElements.size(); i++)
			mElements.get(i).write(mFile);
	}
	
	/**
	 * Adds an entry to the index
	 */
	public int add(T entry) throws IOException
	{
		int insertIndex = getInsertIndex(entry);
		
		mElements.add(insertIndex,entry);
		
		// Check if there is room in the file for this
		if(mLocator.isFreeSpace(getLocation() + getSize(), getEntrySize(), entry))
		{
			// Calculate the new header values
			updateElementCount(mElements.size());
			updateSize(mElements.size() * getEntrySize());

			// Shift the index entries
			write(insertIndex);
			
			Debug.finest("Writing %d " + getIndexName() + " entries from %X -> %X", mElements.size() - insertIndex, getLocation() + insertIndex * getEntrySize(), getLocation() + getSize() - 1);
			
			mLocator.consumeSpace(getLocation() + (mElements.size()-1) * getEntrySize(), getEntrySize());
		}
		else
		{
			long oldLocation = getLocation();
			long oldSize = getSize();
			
			long newSize = mElements.size() * getEntrySize();
			// Find a new location for the index
			long newLocation = mLocator.findFreeSpace(newSize, entry);
			if(newLocation == 0)
				newLocation = mFile.length();

			// Calculate the new header values
			updateElementCount(mElements.size());
			updateSize(newSize);
			updateLocation(newLocation);
			
			// Append the index to the back of the file
			write(0);
			
			Debug.finest(getIndexName() + " relocated from %X -> (%X->%X)", oldLocation, newLocation, newLocation + newSize - 1);
			
			mLocator.releaseSpace(oldLocation, oldSize);
			mLocator.consumeSpace(newLocation, newSize);
		}
		
		// Save the header
		mFile.seek(0);
		mHeader.write(mFile);
		
		return insertIndex;
	}

	/**
	 * Removes the entry from the index
	 */
	public final void remove(T entry) throws IOException
	{
		remove(mElements.indexOf(entry));
	}
	/**
	 * Removes the entry from the index
	 */
	public void remove(int index) throws IOException
	{
		if(index < 0 || index >= mElements.size())
			throw new IllegalArgumentException("Index out of range.");
		
		onRemove(mElements.get(index));
		
		// Shift the existing entries
		mFile.seek(getLocation() + index * getEntrySize());
		for(int i = index + 1; i < mElements.size(); i++)
			mElements.get(i).write(mFile);
		
		mElements.remove(index);
		
		updateSize(mElements.size() * getEntrySize());
		updateElementCount(mElements.size());
		
		mLocator.releaseSpace(mFile.getFilePointer(), getEntrySize());
		
		Debug.finer("Entry %d removed from %s", index, getIndexName());

		// Write the file header
		mFile.seek(0);
		mHeader.write(mFile);
	}
	
	/**
	 * Gets the number of entries in this index
	 */
	public final int getCount()
	{
		return mElements.size();
	}
	
	/**
	 * Gets an 
	 * @param index
	 * @return
	 */
	public T get(int index)
	{
		return mElements.get(index);
	}
	
	public void set(int index, T entry) throws IOException 
	{
		if(index < 0 || index >= mElements.size())
			throw new IllegalArgumentException("Index out of range.");
		
		mElements.set(index, entry);
		
		// Write the changes
		mFile.seek(getLocation() + index * getEntrySize());
		entry.write(mFile);
		
		Debug.finest("Updated entry %d in %s (%X->%X)", index, getIndexName(), getLocation() + index * getEntrySize(), getLocation() + (index+1) * getEntrySize() - 1);
		
	}
			
	public final void relocate(long newLocation) throws IOException
	{
		Utility.shiftBytes(mFile, getLocation(), newLocation, getSize());
		updateLocation(newLocation);
		
		mFile.seek(0);
		mHeader.write(mFile);
	}
	
	@Override
	public Iterator<T> iterator()
	{
		final Iterator<T> it = mElements.iterator();
		return new UnmodifiableIterator<T>()
		{

			@Override
			public boolean hasNext()
			{
				return it.hasNext();
			}

			@Override
			public T next()
			{
				return it.next();
			}
		};
	}
	
	public List<T> getEntries()
	{
		return Collections.unmodifiableList(mElements);
	}
	
	public int indexOf(T entry)
	{
		return mElements.indexOf(entry);
	}
}
