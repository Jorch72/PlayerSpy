package au.com.mineauz.PlayerSpy.structurefile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import au.com.mineauz.PlayerSpy.Utilities.ACIDRandomAccessFile;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.debugging.Profiler;
import au.com.mineauz.PlayerSpy.structurefile.AbstractHoleIndex.HoleData;

public class StructuredFile
{
	private ReentrantReadWriteLock mLock;
	private Index<?>[] mIndexes;
	
	private AbstractHoleIndex mHoleIndex;
	
	protected ACIDRandomAccessFile mFile;
	
	private File mFilePath;
	
	public StructuredFile()
	{
		mLock = new ReentrantReadWriteLock(true);
	}
	
	protected void lockWrite()
	{
		mLock.writeLock().lock();
	}
	
	protected void lockRead()
	{
		mLock.readLock().lock();
	}
	
	protected void unlockWrite()
	{
		mLock.writeLock().unlock();
	}
	
	protected void unlockRead()
	{
		mLock.readLock().unlock();
	}
	
	/**
	 * Begins a transaction in both master and child files.
	 * The order is: master then child
	 */
	protected static void beginJointTransaction(StructuredFile master, StructuredFile child) throws IOException
	{
		master.mFile.beginTransaction();
		child.mFile.beginTransaction(master.mFile);
	}
	
	/**
	 * Commits a transaction in both master and child files.
	 * The order is: child then master
	 */
	protected static void commitJointTransaction(StructuredFile master, StructuredFile child) throws IOException
	{
		master.mFile.commit();
	}
	
	/**
	 * Rolls back a transaction in both master and child files.
	 * The order is: child then master
	 */
	protected static void rollbackJointTransaction(StructuredFile master, StructuredFile child)
	{
		master.mFile.rollback();
	}
	
	protected void load(ACIDRandomAccessFile file, File filePath, Index<?>... indexes)
	{
		mIndexes = indexes;
		
		for(Index<?> index : mIndexes)
		{
			if(index instanceof AbstractHoleIndex)
			{
				if(mHoleIndex != null)
					throw new IllegalArgumentException("Cannot have 2 hole indexes in the file.");
				else
					mHoleIndex = (AbstractHoleIndex)index;
			}
		}
		
		if(mHoleIndex == null)
			throw new IllegalArgumentException("No hole index specified. One is needed for the structured file to work");
		
		mFile = file;
		mFilePath = filePath;
	}
	
	public File getFile()
	{
		return mFilePath;
	}
	
	protected void pullData(long location) throws IOException
	{
		Profiler.beginTimingSection("pullData");
		// Grab what ever is next after this
		long nextLocation;
		long nextSize = 0;
		HoleEntry holeData = mHoleIndex.getHoleAfter(location);
		
		List<IData<?>> allData = getAllData();

		while(holeData != null)
		{
			// Find what data needs to be pulled
			nextLocation = holeData.Location + holeData.Size;
			
			Debug.finest("Pulling data from %X to %X", nextLocation, holeData.Location);
			
			IData<?> selectedData = null;
			
			for(IData<?> data : allData)
			{
				if(data instanceof HoleEntry || (!(data instanceof IMovableData) && !(data instanceof Index)))
					continue;
				
				if(data.getLocation() == nextLocation)
				{
					nextSize = data.getSize();
					selectedData = data;
					break;
				}
			}

			// Pull the data
			if(selectedData != null)
			{
				Utility.shiftBytes(mFile, nextLocation, holeData.Location, nextSize);
				
				HoleEntry old = new HoleEntry();
				old.Location = holeData.Location + nextSize;
				old.Size = holeData.Size;
				
				if(selectedData instanceof Index)
				{
					((Index<?>)selectedData).setLocation(holeData.Location);
					Debug.finest("Shifted %s from %X -> (%X-%X)", ((Index<?>)selectedData).getIndexName(), nextLocation, holeData.Location, holeData.Location + nextSize - 1);
				}
				else
				{
					((IMovableData<?>)selectedData).setLocation(holeData.Location);
					((IMovableData<?>)selectedData).saveChanges();
					Debug.finest("Shifted %s from %X -> (%X-%X)", ((IMovableData<?>)selectedData).toString(), nextLocation, holeData.Location, holeData.Location + nextSize - 1);
				}
				
				// Move the hole
				mHoleIndex.remove(holeData);
				
				// Add in the new hole
				mHoleIndex.add(old);
				
				// Attempt to compact further stuff
				pullData(old.Location);
			}
			
			HoleEntry lastHole = holeData;
			holeData = mHoleIndex.getHoleAfter(location);
			
			if(holeData == null && lastHole != null)
			{
				// Nothing to pull because there is no more data after us
				mHoleIndex.remove(lastHole);
				// Trim the file
				mFile.setLength(lastHole.Location);
			}
		}
		Profiler.endTimingSection();
	}
	
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	protected List<IData<?>> getAllData()
	{
		TreeMap<Long, IData<?>> data = new TreeMap<Long, IData<?>>(); 
		
		for(Index<?> index : mIndexes)
		{
			data.put(index.getLocation(), index);
			
			if(index instanceof DataIndex)
			{
				for(IndexEntry entry : index)
				{
					IData<?> d = ((DataIndex)index).getDataFor(entry);
					if(d.getSize() == 0)
						continue;
					data.put(d.getLocation(), d);
				}
			}
		}

		return new ArrayList<IData<?>>(data.values());
	}
	
	/**
	 * Debugging method used to check whether a space is really free, instead of believing what the hole index says
	 */
	public void checkSpaceStatus(long location, long size, boolean free)
	{
		List<IData<?>> data = getAllData();
		
		for(IData<?> item : data)
		{
			if(item instanceof HoleData)
				continue;
			
			if((location >= item.getLocation() && location < item.getLocation() + item.getSize()) ||
				(location + size > item.getLocation() && location + size < item.getLocation() + item.getSize()) ||
				(location < item.getLocation() && location + size > item.getLocation() + item.getSize()))
			{
				if(free)
					throw new RuntimeException(String.format("Holes indicate that this section is free. But absolute scan says othewise. Location: %X->%X. Space occupied by %s from %X->%X", location, location + size - 1, item.getClass().getSimpleName(), item.getLocation(), item.getLocation() + item.getSize()-1));
				else
					return;
			}
		}
		
		if(!free)
			throw new RuntimeException(String.format("Holes indicate that this section is not free. But absolute scan says othewise. Location: %X->%X", location, location + size - 1));
	}
}
