package au.com.mineauz.PlayerSpy.monitoring;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.bukkit.Location;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.debugging.Debug;

/**
 * Tracks the spread of blocks. Does not track the spread of blocks between reloads.
 * @author schmoller
 *
 */
public class SpreadTracker 
{
	/**
	 * The delay in ms that will be waited before removing the block.
	 * Default: 5 minutes
	 */
	public static long sCheckDelay = 300000;
	
	public SpreadTracker()
	{
		mSources = new HashMap<Location, Cause>();
		mReferenceCount = new HashMap<Location, Integer>();
		mMap = new HashMap<Location, Location>();
		mCheckPriority = new TreeMap<Long, Location>();
		mCheckMap = new HashMap<Location, Long>();
	}
	/**
	 * Records the start of a spread
	 * @param loc The location to start
	 * @param cause The cause of the spread
	 */
	public void addSource(Location loc, Cause cause)
	{
		Location cloned = loc.clone(); 
		mSources.put(cloned, cause);
		mMap.put(cloned, cloned);
		mReferenceCount.put(cloned,1);
		
		long checkTime = Calendar.getInstance().getTimeInMillis() + sCheckDelay;
		mCheckPriority.put(checkTime, cloned);
		mCheckMap.put(cloned, checkTime);
		
		Debug.finer("Spread source added by " + cause + " (" + loc.getBlock().getType().toString() + ")");
	}
	/**
	 * Updates a source with newly found info about what caused it
	 * @param loc The location of the source
	 * @param newCause The new cause to set
	 */
	public void updateSource(Location loc, Cause newCause)
	{
		if(mSources.containsKey(loc))
			mSources.get(loc).update(newCause);
	}
	public void cleanupSource(Location sourceLoc)
	{
		Iterator<Entry<Location, Location>> it = mMap.entrySet().iterator();
		while(it.hasNext())
		{
			Entry<Location, Location> data = it.next();
			
			if(data.getValue().equals(sourceLoc))
			{
				// Remove the scheduled delete
				Long key = mCheckMap.remove(data.getKey());
				if(key != null)
					mCheckPriority.remove(key);
				
				it.remove();
			}
		}
		mSources.remove(sourceLoc);
	}
	
	public boolean spreadTo(Location from, Location to)
	{
		// Lookup to see if the source was recorded
		Location sourceLocation = mMap.get(from);
		// Map the destination block back to the source block
		if(sourceLocation != null && mSources.containsKey(sourceLocation))
		{
			Debug.finer("Spreading " + from.getBlock().getType().toString() + " from " + Utility.locationToStringShort(from) + " to " + Utility.locationToStringShort(to) + " thanks to " + mSources.get(sourceLocation));
			Location loc = to.clone();
			mMap.put(loc, sourceLocation);
			mReferenceCount.put(sourceLocation, mReferenceCount.get(sourceLocation) + 1);
			
			// Schedule a remove
			long checkTime = Calendar.getInstance().getTimeInMillis() + sCheckDelay;
			mCheckPriority.put(checkTime, loc);
			mCheckMap.put(loc, checkTime);
			
			return true;
		}
		
		return false;
	}
	/**
	 * Used when a block that was spread is removed 
	 * @param loc The location of the block removed
	 */
	public void remove(Location loc)
	{
		Location sourceLocation = mMap.get(loc);
		
		if(sourceLocation != null && mSources.containsKey(sourceLocation))
		{
			Debug.finer("Removing @" + Utility.locationToStringShort(loc));
			mMap.remove(loc);

			// Remove the scheduled delete
			Long key = mCheckMap.remove(loc);
			if(key != null)
				mCheckPriority.remove(key);
			
			// Update the reference count
			int count = mReferenceCount.get(sourceLocation) - 1;
			if(count > 0)
				mReferenceCount.put(sourceLocation, count);
			else
			{
				// remove the source since nothing points to it anymore
				mReferenceCount.remove(sourceLocation);
				mSources.remove(sourceLocation);
				Debug.finer("Removed source");
			}
		}
	}
	
	/**
	 * Finds who caused the change at the specified location. Only works for blocks that have spread with spreadTo() and were registered initially with addSource()
	 * @param loc The location to check
	 * @return The cause of it
	 */
	public Cause getCause(Location loc)
	{
		Location sourceLocation = mMap.get(loc);
		
		if(sourceLocation == null)
			return null;
		
		return mSources.get(sourceLocation);
	}
	
	/**
	 * Handles removing spread entries after a certain period of time, sCheckDelay, from their creation
	 * Done to conserve memory
	 */
	public void doUpdate()
	{
		if(mCheckPriority.isEmpty())
			return;
		
		long time = Calendar.getInstance().getTimeInMillis();
		while(mCheckPriority.firstKey() <= time)
		{
			// Remove the scheduled remove
			Location loc = mCheckPriority.firstEntry().getValue();
			mCheckMap.remove(loc);
			mCheckPriority.remove(mCheckPriority.firstKey());
			
			// remove the block
			remove(loc);
			
			if(mCheckPriority.isEmpty())
				break;
		}
			
	}
	
	// All the source blocks
	private HashMap<Location, Cause> mSources;
	private HashMap<Location, Integer> mReferenceCount;
	
	// A map of locations to the source
	private HashMap<Location, Location> mMap;
	// An ordered list with the smallest element being the closest one (in time) to check
	private TreeMap<Long, Location> mCheckPriority;
	// A map of all items in mMap to one in mCheckPriority
	private HashMap<Location, Long> mCheckMap;
}
