package au.com.mineauz.PlayerSpy.LogTasks;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.bukkit.Location;

import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Utility;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex.SessionInFile;

/**
 * A task that finds the history of a block
 */
public class BlockHistoryTask implements Callable<HashMap<String, RecordList>> 
{
	private Location mLocation;
	private long mStartTime;
	private long mEndTime;
	private String mFilter;
	
	public BlockHistoryTask(Location loc)
	{
		this(loc, 0, Long.MAX_VALUE, null);
	}
	public BlockHistoryTask(Location loc, String filter)
	{
		this(loc, 0, Long.MAX_VALUE, filter);
	}
	public BlockHistoryTask(Location loc, long start, long end)
	{
		this(loc, start, end, null);
	}
	public BlockHistoryTask(Location loc, long start, long end, String filter)
	{
		mLocation = loc.clone();
		mStartTime = start;
		mEndTime = end;
		mFilter = filter;
	}
	

	@Override
	public HashMap<String, RecordList> call() throws Exception 
	{
		LogUtil.fine("Searching for block records @" + Utility.locationToString(mLocation) + " for " + (mFilter == null ? "anyone" : mFilter));
		HashMap<String, RecordList> results = new HashMap<String, RecordList>();
		
		// Grab all the applicable sessions
		// TODO: Make a version of this function that allows retrieving only sessions for a specific file
		List<SessionInFile> allSessions = CrossReferenceIndex.instance.getSessionsFor(mLocation.getChunk());
		
		for(SessionInFile fileSession : allSessions)
		{
			// Make sure its ok to use this file
			if(mFilter != null)
			{
				if(mFilter.startsWith("#") && !fileSession.Log.getName().equals("__global"))
					continue;
				if(!fileSession.Log.getName().equalsIgnoreCase(mFilter))
					continue;
			}
			// Get the recordlist to put the results on
			RecordList target = null;
			
			String targetName = fileSession.Log.getName();
			if(fileSession.Log.isUsingOwnerTags())
				targetName = fileSession.Log.getOwnerTag(fileSession.Session);
			
			if(!results.containsKey(targetName))
				results.put(targetName, new RecordList());
			
			target = results.get(targetName);
			
			// Load up the records in the session
			RecordList source = fileSession.Log.loadSession(fileSession.Session);
			
			// Find all block change records at that location
			for(Record record : source)
			{
				if(record.getType() != RecordType.BlockChange)
					continue;
				
				if(record.getTimestamp() < mStartTime)
					continue;
				if(record.getTimestamp() > mEndTime)
					break;
				
				if(((BlockChangeRecord)record).getLocation().getWorld() == mLocation.getWorld())
				{
					if(((BlockChangeRecord)record).getLocation().distance(mLocation) < 1)
						target.add(record);
				}
			}
		}

		CrossReferenceIndex.instance.releaseLastLogs();
		
		LogUtil.fine("Search complete");
		return results;
	}
}
