package au.com.mineauz.PlayerSpy.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.Pair;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex.SessionInFile;

@Deprecated
public class SearchAndLogTask implements Callable<Boolean>
{
	public Location mLocation;
	public RecordList mRecords;
	public String mCause;
	
	public SearchAndLogTask(Location location, RecordList records, String cause)
	{
		mLocation = location;
		mRecords = records;
		mCause = cause;
	}
	@Override
	public Boolean call() throws Exception 
	{
		HashMap<String, RecordList> results = new HashMap<String, RecordList>();
		// Add in any records that are yet to be written to file
		for(ShallowMonitor mon : GlobalMonitor.instance.getAllMonitors())
		{
			List<Pair<String, RecordList>> inBuffer = mon.getBufferedRecords();
			for(Pair<String, RecordList> pair : inBuffer)
			{
				if(!results.containsKey(mon.getMonitorTarget().getName()))
					results.put(mon.getMonitorTarget().getName(), new RecordList());
				
				RecordList target = results.get(mon.getMonitorTarget().getName());
				
				for(Record record : pair.getArg2())
				{
					if(record.getType() != RecordType.BlockChange)
						continue;

					if(!((BlockChangeRecord)record).wasPlaced())
						continue;
					
					if(((BlockChangeRecord)record).getLocation().getWorld() == mLocation.getWorld())
					{
						if(((BlockChangeRecord)record).getLocation().distance(mLocation) < 1)
							target.add(record);
					}
				}
			}
		}
		
		// Grab all the applicable sessions
		List<SessionInFile> allSessions = CrossReferenceIndex.instance.getSessionsFor(mLocation.getChunk());
		
		LogUtil.finer("Found " + allSessions.size() + " possible session matches. Attempting to narrow");
		for(SessionInFile fileSession : allSessions)
		{
			// Get the recordlist to put the results on
			RecordList target = null;
			
			String targetName = fileSession.Log.getName();
			if(targetName.equals("__global"))
				continue;
			
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

				if(!((BlockChangeRecord)record).wasPlaced())
					continue;
				
				if(((BlockChangeRecord)record).getLocation().getWorld() == mLocation.getWorld())
				{
					if(((BlockChangeRecord)record).getLocation().distance(mLocation) < 1)
						target.add(record);
				}
			}
		}
		
		// Now find the latest one
		ArrayList<Pair<String, Record>> mostRecent = new ArrayList<Pair<String, Record>>();
		
		for(Entry<String, RecordList> ent : results.entrySet())
		{
			for(Record record : ent.getValue())
			{
				// Attempt to put them into the most recent list
				boolean added = false;
				for(int i = 0; i < mostRecent.size(); i++)
				{
					if(record.getTimestamp() > mostRecent.get(i).getArg2().getTimestamp())
					{
						mostRecent.add(i,new Pair<String, Record>(ent.getKey(), record));
						added = true;
						break;
					}
				}
				if(!added && mostRecent.size() < 1)
					mostRecent.add(new Pair<String, Record>(ent.getKey(), record));
				else if(added && mostRecent.size() > 1)
					mostRecent.remove(mostRecent.size()-1);
			}
		}
		
		boolean logged = false;
		if(!mostRecent.isEmpty())
		{
			String playerName = mostRecent.get(0).getArg1();
			
			int index = playerName.indexOf(">");
			if(index != -1)
				playerName = playerName.substring(0, index);

			LogUtil.finer("Logging the records for " + mCause + " against " + playerName);
			
			OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
			
			ShallowMonitor mon = GlobalMonitor.instance.getMonitor(player);
			if(mon != null)
			{
				for(Record record : mRecords)
					mon.logRecord(record, mCause);
				
				logged = true;
			}
			else
			{
				// Ok now get the log file for them
				LogFile log = LogFileRegistry.getLogFile(Bukkit.getOfflinePlayer(playerName));
				
				if(log != null)
				{
					if(mCause == null)
						log.appendRecords(mRecords);
					else
						log.appendRecords(mRecords, mCause);
					
					logged = true;
					LogFileRegistry.unloadLogFile(Bukkit.getOfflinePlayer(playerName));
				}
			}
		}
		
		if(!logged && mCause != null)
		{
//			LogUtil.finer("Logging the records for " + mCause + " against __global");
//			LogFile log = null;
//			log = LogFileRegistry.getGlobalLog();
//			log.appendRecords(mRecords, mCause);
//			log.close();
//			if(!log.isLoaded())
//				LogFileRegistry.unloadGlobalLogFile();
		}
		
		CrossReferenceIndex.instance.releaseLastLogs();
		return null;
	}

}
