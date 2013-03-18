package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.debugging.Debug;

public abstract class DataIndex<T extends IndexEntry, Y extends IMovableData<T>> extends Index<T>
{
	public DataIndex( LogFile log, FileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(log, header, file, locator);
	}

	/**
	 * Creates a new data element from an index entry
	 */
	protected abstract Y getDataFor(T entry);

	public void relocateData(Y data, long toLocation) throws IOException
	{
		Debug.loggedAssert(data != null);
		
		T indexEntry = data.getIndexEntry();
		int index = indexOf(indexEntry);
		
		// Move it
		Utility.shiftBytes(mFile, data.getLocation(), toLocation, data.getSize());
		
		// Update the location
		data.setLocation(toLocation);
		set(index, indexEntry);
	}
}
