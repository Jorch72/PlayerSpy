package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.DebugHelper;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.LogTasks.Task;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;
import au.com.mineauz.PlayerSpy.monitoring.ShallowMonitor;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex.SessionInFile;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;
import au.com.mineauz.PlayerSpy.search.interfaces.FormatterModifier;
import au.com.mineauz.PlayerSpy.search.interfaces.Modifier;

public class SearchTask implements Task<SearchResults>
{
	private SearchFilter mFilter;
	
	private long startTime = 0, endTime = Long.MAX_VALUE;
	private SearchResults results;
	private HashMap<Cause, Integer> reverseCauseMap = new HashMap<Cause, Integer>();
	private int nextCauseId = 0;
	
	private static final Comparator<Pair<Record,Integer>> sForwardComparator;
	@SuppressWarnings( "unused" )
	private static final Comparator<Pair<Record,Integer>> sReverseComparator;
	
	private Comparator<Pair<Record,Integer>> activeComparator;
	private long minDate = 0;
	
	// debug data
	private int totalRecords = 0;
	
	static
	{
		sForwardComparator = new Comparator<Pair<Record,Integer>>() 
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
		};
		sReverseComparator = new Comparator<Pair<Record,Integer>>() 
		{

			@Override
			public int compare(Pair<Record, Integer> a, Pair<Record, Integer> b) 
			{
				if(a.getArg1().getTimestamp() < b.getArg1().getTimestamp())
					return -1;
				if(a.getArg1().getTimestamp() > b.getArg1().getTimestamp())
					return 1;
				
				return 0;
			}
		};
	}
	
	public SearchTask(SearchFilter filter)
	{
		mFilter = filter;
		activeComparator = sForwardComparator;
	}
	
	private void insertRecord(Record record, int id)
	{
		Pair<Record,Integer> toInsert = new Pair<Record, Integer>(record, id);
		
		if(results.allRecords.size() != 0)
		{
			int index = Collections.binarySearch(results.allRecords,toInsert,activeComparator);
			if(index < 0)
				index = -index - 1;
			
			//if(index == results.allRecords.size())
			results.allRecords.add(index,toInsert);
		}
		
		else
			results.allRecords.add(toInsert);
		
		// Remove the last item if its too big
		if(!mFilter.noLimit && results.allRecords.size() > SpyPlugin.getSettings().maxSearchResults)
			results.allRecords.remove(results.allRecords.size()-1);
		
		minDate = results.allRecords.get(results.allRecords.size()-1).getArg1().getTimestamp();
	}
	
	private void processRecords(RecordList records, Cause cause)
	{
        if(records.size() == 0)
            return;
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
		
		// Filter out the records we want
		ListIterator<Record> it = records.listIterator(records.size()-1);
		
		while(it.hasPrevious())
		{
			Record record = it.previous();
			totalRecords++;
			
			// First, time
			if(record.getTimestamp() < startTime)
				break;
			if(record.getTimestamp() < minDate && results.allRecords.size() >= SpyPlugin.getSettings().maxSearchResults)
				break;
			
			if(record.getTimestamp() > endTime)
				continue;
			
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
			insertRecord(record, id);
			addedRecords = true;
		}
		
		if(addedRecords)
			results.causes.put(id, cause);
	}
	@Override
	public SearchResults call()
	{
		CrossReferenceIndex.Results sessionsToSearch = null;
		
		// Get the contraints
		
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
		
		results = new SearchResults();
		results.causes = new HashMap<Integer, Cause>();
		results.allRecords = new ArrayList<Pair<Record,Integer>>();
		results.usedFilter = mFilter;
		
		// Debug stats:
		int sessionCount = 0;
		int bufferedCount = 0;
		totalRecords = 0;
		
		// Search buffers
		for(ShallowMonitor mon : GlobalMonitor.instance.getAllMonitors())
		{
			List<Pair<String, RecordList>> inBuffer = mon.getBufferedRecords();
			for(Pair<String, RecordList> pair : inBuffer)
			{
				Cause cause;
				if(pair.getArg1() != null)
					cause = Cause.playerCause(mon.getMonitorTarget(), pair.getArg1());
				else
					cause = Cause.playerCause(mon.getMonitorTarget());
				
				// Check the constraints
				if(mFilter.causes.size() != 0)
				{
					boolean constraintOk = false;
					for(Cause testCause : mFilter.causes)
					{
						if(cause.equals(testCause) || (testCause.isGlobal() && testCause.getExtraCause().equalsIgnoreCase(cause.getExtraCause())))
						{
							constraintOk = true;
							break;
						}
					}
					
					if(!constraintOk)
						continue;
				}
				bufferedCount++;
				sessionCount++;
				// Load up the records in the session
				RecordList source = pair.getArg2();

				processRecords(source, cause);
			}
		}
		
		// Global records
		for(World world : Bukkit.getWorlds())
		{
			HashMap<String, RecordList> buffers = GlobalMonitor.instance.getBufferForWorld(world);
			for(Entry<String, RecordList> buffer : buffers.entrySet())
			{
				Cause cause = Cause.globalCause(world, buffer.getKey());
				
				// Check the constraints
				if(mFilter.causes.size() != 0)
				{
					boolean constraintOk = false;
					for(Cause testCause : mFilter.causes)
					{
						if(cause.equals(testCause) || (testCause.isGlobal() && testCause.getExtraCause().equalsIgnoreCase(cause.getExtraCause())))
						{
							constraintOk = true;
							break;
						}
					}
					
					if(!constraintOk)
						continue;
				}
				bufferedCount++;
				sessionCount++;
				// Load up the records in the session
				processRecords(buffer.getValue(),cause);
			}
		}
		
		// Pending records
		for(Pair<RecordList,Cause> pending : GlobalMonitor.instance.getPendingRecords().values())
		{
			// Check the constraints
			if(mFilter.causes.size() != 0)
			{
				boolean constraintOk = false;
				for(Cause testCause : mFilter.causes)
				{
					if(pending.getArg2().equals(testCause) || (testCause.isGlobal() && testCause.getExtraCause().equalsIgnoreCase(pending.getArg2().getExtraCause())))
					{
						constraintOk = true;
						break;
					}
				}
				
				if(!constraintOk)
					continue;
			}
			
			bufferedCount++;
			sessionCount++;
			
			// Load up the records in the session
			processRecords(pending.getArg1(), pending.getArg2());
		}
		
		
		sessionsToSearch = CrossReferenceIndex.instance.getSessionsFor(startTime, endTime);
		
		for(SessionInFile fileSession : sessionsToSearch.foundSessions)
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
			
			sessionCount++;
			// Check the constraints
			if(mFilter.causes.size() != 0)
			{
				boolean constraintOk = false;
				for(Cause testCause : mFilter.causes)
				{
					if(cause.equals(testCause) || (testCause.isGlobal() && testCause.getExtraCause().equalsIgnoreCase(cause.getExtraCause())))
					{
						constraintOk = true;
						break;
					}
				}
				
				if(!constraintOk)
					continue;
			}
			
			// Dont bother loading up ones that dont add anything
			if(fileSession.Session.EndTimestamp < minDate && results.allRecords.size() >= SpyPlugin.getSettings().maxSearchResults)
				continue;
			
			// Load up the records for the session
			RecordList records = fileSession.Log.loadSession(fileSession.Session);
			
			processRecords(records, cause);
		}
		
		DebugHelper.debugMessage(String.format("Search: LC:%d SC:%d BC:%d RC:%d",sessionsToSearch.getLogCount(),sessionCount,bufferedCount,totalRecords));
		sessionsToSearch.release();
		
		for(Modifier modifier : mFilter.modifiers)
		{
			if(modifier instanceof FormatterModifier)
			{
				((FormatterModifier)modifier).format(results);
			}
		}
		
		return results;
	}

	@Override
	public int getTaskTargetId() 
	{
		return 999999999;
	}

}
