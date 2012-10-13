package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.Pair;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.LogTasks.Task;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex.SessionInFile;

public class SearchTask implements Task<SearchResults>
{
	private SearchFilter mFilter;
	public SearchTask(SearchFilter filter)
	{
		mFilter = filter;
	}
	@SuppressWarnings("deprecation")
	@Override
	public SearchResults call()
	{
		List<SessionInFile> sessionsToSearch = null;
		
		// Get the contraints
		long startTime = 0, endTime = Long.MAX_VALUE;

		for(Constraint constraint : mFilter.andConstraints)
		{
			if(constraint instanceof DateConstraint)
			{
				startTime = ((DateConstraint)constraint).startDate.getTime();
				endTime = ((DateConstraint)constraint).endDate.getTime();
				mFilter.andConstraints.remove(constraint);
				break;
			}
		}
		
		SearchResults results = new SearchResults();
		results.causes = new HashMap<Integer, Cause>();
		results.allRecords = new ArrayList<Pair<Record,Integer>>();
		int nextCauseId = 0;
		
		HashMap<Cause, Integer> reverseCauseMap = new HashMap<Cause, Integer>();
		
		sessionsToSearch = CrossReferenceIndex.instance.getSessionsFor(startTime, endTime);
		
		for(SessionInFile fileSession : sessionsToSearch)
		{
			Cause cause;
			
			// Build the cause
			String ownerTag = fileSession.Log.getOwnerTag(fileSession.Session);
			
			if(fileSession.Log.getName().startsWith(LogFileRegistry.cGlobalFilePrefix))
			{
				if(ownerTag == null)
					// :( shouldnt happen but does
					continue;
				
				cause = Cause.globalCause(Bukkit.getWorld(fileSession.Log.getName().substring(LogFileRegistry.cGlobalFilePrefix.length())), ownerTag);
			}
			else
			{
				if(ownerTag == null)
					cause = Cause.playerCause(Bukkit.getOfflinePlayer(fileSession.Log.getName()));
				else
					cause = Cause.playerCause(Bukkit.getOfflinePlayer(fileSession.Log.getName()), ownerTag);
			}
			
			// Check the constraints
			if(mFilter.causes.size() != 0)
			{
				boolean constraintOk = false;
				for(Cause testCause : mFilter.causes)
				{
					if(cause.equals(testCause) || ((cause.isGlobal() && testCause.isGlobal()) && cause.getExtraCause().equalsIgnoreCase(testCause.getExtraCause())))
					{
						constraintOk = true;
						break;
					}
				}
				
				if(!constraintOk)
					continue;
			}
			
			// Get the cause id
			int id;
			boolean addedRecords = false;
			if(reverseCauseMap.containsKey(cause))
				id = reverseCauseMap.get(cause);
			else
			{
				id = nextCauseId++;
				reverseCauseMap.put(cause, id);
			}
			
			// Load up the records for the session
			RecordList records = fileSession.Log.loadSession(fileSession.Session);
			
			// Filter out the records we want
			for(Record record : records)
			{
				// First, time
				if(record.getTimestamp() < startTime)
					continue;
				
				if(record.getTimestamp() > endTime)
					break;
				
				boolean ok = true;
				// Do the and constraints
				if(!mFilter.andConstraints.isEmpty())
				{
					for(Constraint constraint : mFilter.andConstraints)
					{
						if(!constraint.matches(record))
						{
							ok = false;
							break;
						}
					}
					if(!ok)
						continue;
				}
				
				// Now do the or constraints
				if(!mFilter.orConstraints.isEmpty())
				{
					ok = false;
					for(Constraint constraint : mFilter.orConstraints)
					{
						if(constraint.matches(record))
						{
							ok = true;
							break;
						}
					}
					if(!ok)
						continue;
				}
				
				// Passed all the constraints
				results.allRecords.add(new Pair<Record, Integer>(record, id));
				addedRecords = true;
			}
			
			if(addedRecords)
				results.causes.put(id, cause);
			
		}
		
		CrossReferenceIndex.instance.releaseLastLogs();
		
		Collections.sort(results.allRecords, new Comparator<Pair<Record,Integer>>() 
		{

			@Override
			public int compare(Pair<Record, Integer> a, Pair<Record, Integer> b) 
			{
				if(a.getArg1().getTimestamp() < b.getArg1().getTimestamp())
					return 1;
				if(a.getArg1().getTimestamp() > b.getArg1().getTimestamp())
					return -1;
				
				return 0;
			}
		});
		
		return results;
	}

	@Override
	public int getTaskTargetId() 
	{
		return 999999999;
	}

}
