package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.LogTasks.Task;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;
import au.com.mineauz.PlayerSpy.monitoring.ShallowMonitor;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex.SessionInFile;
import au.com.mineauz.PlayerSpy.search.interfaces.CauseConstraint;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;
import au.com.mineauz.PlayerSpy.search.interfaces.FormatterModifier;
import au.com.mineauz.PlayerSpy.search.interfaces.Modifier;
import au.com.mineauz.PlayerSpy.tracdata.LogFileRegistry;

public class SearchTask extends Task<SearchResults>
{
	private SearchFilter mFilter;
	
	private long startTime = 0, endTime = Long.MAX_VALUE;
	private SearchResults results;
	private HashMap<Cause, Integer> reverseCauseMap = new HashMap<Cause, Integer>();
	private int nextCauseId = 0;
	private Comparator<Pair<Record,Integer>> comparator = sForwardComparator;
	
	private static final Comparator<Pair<Record,Integer>> sForwardComparator;
	private static final Comparator<Pair<Record,Integer>> sReverseComparator;
	
	private long minDate = 0;
	private boolean reversed = false;
	
	private int lastProgress = 0;
	
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
	}
	
	private void insertRecord(Record record, int id)
	{
		Pair<Record,Integer> toInsert = new Pair<Record, Integer>(record, id);
		
		if(results.allRecords.size() != 0)
		{
			int index = Collections.binarySearch(results.allRecords,toInsert,comparator);
			if(index < 0)
				index = -index - 1;
			
			results.allRecords.add(index,toInsert);
		}
		
		else
			results.allRecords.add(toInsert);
		
		// Remove the last item if its too big
		if(!mFilter.noLimit && results.allRecords.size() > SpyPlugin.getSettings().maxSearchResults)
			results.allRecords.remove(results.allRecords.size()-1);
		
		minDate = results.allRecords.get(results.allRecords.size()-1).getArg1().getTimestamp();
	}
	
	private boolean causeMatches(Cause cause)
	{
		if(mFilter.causes.isEmpty())
			return true;
		
		for(CauseConstraint constraint : mFilter.causes)
		{
			if(!constraint.matches(cause))
				return false;
		}
		
		return true;
	}
	
	private boolean processForward(RecordList records, int id)
	{
		boolean addedRecords = false;
		ListIterator<Record> it = records.listIterator(records.size()-1);
		
		while(it.hasPrevious())
		{
			Record record = it.previous();
			totalRecords++;
			
			// First, time
			if(record.getTimestamp() < startTime)
				break;
			if(!mFilter.noLimit && results.allRecords.size() >= SpyPlugin.getSettings().maxSearchResults && record.getTimestamp() < minDate)
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
			
			// Passed all the constraints
			insertRecord(record, id);
			addedRecords = true;
		}
		
		return addedRecords;
	}
	private boolean processBackward(RecordList records, int id)
	{
		boolean addedRecords = false;
		ListIterator<Record> it = records.listIterator(0);
		
		while(it.hasNext())
		{
			Record record = it.next();
			totalRecords++;
			
			// First, time
			if(record.getTimestamp() > endTime)
				break;
			if(!mFilter.noLimit && results.allRecords.size() >= SpyPlugin.getSettings().maxSearchResults && record.getTimestamp() > minDate)
				break;
			
			if(record.getTimestamp() < startTime)
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
			
			// Passed all the constraints
			insertRecord(record, id);
			addedRecords = true;
		}
		
		return addedRecords;
	}
	
	private void processRecords(RecordList records, Cause cause)
	{
        if(records == null || records.size() == 0)
            return;
		// Get the cause id
		int id;
		if(reverseCauseMap.containsKey(cause))
			id = reverseCauseMap.get(cause);
		else
		{
			id = nextCauseId++;
			reverseCauseMap.put(cause, id);
		}
		
		// Filter out the records we want
		if(reversed)
		{
			if(processBackward(records, id))
				results.causes.put(id, cause);
		}
		else
		{
			if(processForward(records, id))
				results.causes.put(id, cause);
		}
	}
	
	@Override
	public SearchResults call()
	{
		CrossReferenceIndex.Results sessionsToSearch = null;
		Debug.info("Beginning search");
		
		// Extract the time range from the constraints to speed up retrieval
		Set<Constraint> toRemove = new HashSet<Constraint>();
		DistanceConstraint distConstraint = null;
		for(Constraint constraint : mFilter.andConstraints)
		{
			if(constraint instanceof TimeConstraint)
			{
				if(((TimeConstraint)constraint).isAfter())
					startTime = ((TimeConstraint)constraint).getTime();
				else
					endTime = ((TimeConstraint)constraint).getTime();
				
				toRemove.add(constraint);
			}
			else if (constraint instanceof DistanceConstraint)
			{
				distConstraint = ((DistanceConstraint)constraint);
			}
		}
		
		mFilter.andConstraints.removeAll(toRemove);
		
		Set<Modifier> toRemoveMods = new HashSet<Modifier>();
		
		for(Modifier mod : mFilter.modifiers)
		{
			if(mod instanceof ReverserFormatter)
			{
				comparator = sReverseComparator;
				reversed = true;
				toRemoveMods.add(mod);
			}
		}
		
		mFilter.modifiers.removeAll(toRemoveMods);
		
		results = new SearchResults();
		results.causes = new HashMap<Integer, Cause>();
		results.allRecords = new ArrayList<Pair<Record,Integer>>();
		results.usedFilter = mFilter;
		
		// Debug stats:
		int sessionCount = 0;
		int bufferedCount = 0;
		totalRecords = 0;
		
		Debug.fine("*Searching active buffers");
		// Search buffers
		for(ShallowMonitor mon : GlobalMonitor.instance.getAllMonitors())
		{
			List<Pair<String, RecordList>> inBuffer = mon.getBufferedRecords();
			synchronized(inBuffer)
			{
				for(Pair<String, RecordList> pair : inBuffer)
				{
					Cause cause;
					if(pair.getArg1() != null)
						cause = Cause.playerCause(mon.getMonitorTarget(), pair.getArg1());
					else
						cause = Cause.playerCause(mon.getMonitorTarget());
					
					// Check the constraints
					if(!causeMatches(cause))
						continue;
					
					bufferedCount++;
					sessionCount++;
					// Load up the records in the session
					RecordList source = (RecordList)pair.getArg2().clone();
	
					processRecords(source, cause);
				}
			}
		}
		
		Debug.fine("*Searching active global buffers");
		// Global records
		for(World world : Bukkit.getWorlds())
		{
			Map<String, RecordList> buffers = GlobalMonitor.instance.getBufferForWorld(world);
			
			synchronized(buffers)
			{
				for(Entry<String, RecordList> buffer : buffers.entrySet())
				{
					Cause cause = Cause.globalCause(world, buffer.getKey());
					
					// Check the constraints
					if(!causeMatches(cause))
						continue;
					
					bufferedCount++;
					sessionCount++;
					// Load up the records in the session
					processRecords((RecordList)buffer.getValue().clone(),cause);
				}
			}
		}
		
		Debug.fine("*Searching pending records");
		
		synchronized(GlobalMonitor.instance.getPendingRecords())
		{
			Debug.fine("Starting iteration over pending");
			int i = 0;
			// Pending records
			for(Pair<RecordList,Cause> pending : GlobalMonitor.instance.getPendingRecords().values())
			{
				Debug.fine("Iteration %d", i++);
				
				// Check the constraints
				if(!causeMatches(pending.getArg2()))
					continue;
				
				bufferedCount++;
				sessionCount++;
				
				// Load up the records in the session
				processRecords((RecordList)pending.getArg1().clone(), pending.getArg2());
			}
		}
		
		Debug.fine("*Searching index for possibles");
		
		if(distConstraint != null)
			sessionsToSearch = CrossReferenceIndex.getSessionsIn(startTime, endTime, distConstraint.location, distConstraint.distance, true);
		else
			sessionsToSearch = CrossReferenceIndex.getSessionsFor(startTime, endTime);
		
		ArrayList<SessionInFile> orderedSessions = new ArrayList<SessionInFile>();
		
		for(SessionInFile fileSession : sessionsToSearch.foundSessions)
		{
			if(orderedSessions.isEmpty())
			{
				orderedSessions.add(fileSession);
				continue;
			}
			
			int index = 0;
			if(reversed)
			{
				index = Collections.binarySearch(orderedSessions,fileSession, new Comparator<SessionInFile>()
				{
					@Override
					public int compare( SessionInFile o1, SessionInFile o2 )
					{
						if(o1.Session.StartTimestamp > o2.Session.StartTimestamp)
							return 1;
						else if(o1.Session.StartTimestamp < o2.Session.StartTimestamp)
							return -1;
						else
						{
							if(o1.Session.EndTimestamp > o2.Session.EndTimestamp)
								return 1;
							else if(o1.Session.EndTimestamp < o2.Session.EndTimestamp)
								return -1;
							return 0;
						}
					}
				});
				if(index < 0)
					index = -index - 1;
			}
			else
			{
				index = Collections.binarySearch(orderedSessions,fileSession, new Comparator<SessionInFile>()
				{
					@Override
					public int compare( SessionInFile o1, SessionInFile o2 )
					{
						if(o1.Session.EndTimestamp > o2.Session.EndTimestamp)
							return -1;
						else if(o1.Session.EndTimestamp < o2.Session.EndTimestamp)
							return 1;
						else
						{
							if(o1.Session.StartTimestamp > o2.Session.StartTimestamp)
								return -1;
							else if(o1.Session.StartTimestamp < o2.Session.StartTimestamp)
								return 1;
							return 0;
						}
					}
				});
				if(index < 0)
					index = -index - 1;
			}
			
			orderedSessions.add(index, fileSession);
		}
		
		Debug.finer("**%d possible sessions found", sessionsToSearch.foundSessions.size());
		for(SessionInFile fileSession : orderedSessions)
		{
			Cause cause;
			
			// Build the cause
			String ownerTag = fileSession.Log.getOwnerTag(fileSession.Session);
			
			if(fileSession.Log.requiresOwnerTags())
			{
				if(ownerTag == null)
				{
					Debug.severe("Sesison %d in log file %s which requires owner tags does not have an owner tag with it.", fileSession.Session.Id, fileSession.Log.getName());
					continue;
				}

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
			if(!causeMatches(cause))
				continue;
			
			// Dont bother loading up ones that dont add anything
			if(!mFilter.noLimit && results.allRecords.size() >= SpyPlugin.getSettings().maxSearchResults)
			{
				if ((reversed && fileSession.Session.StartTimestamp > minDate) || 
					(!reversed && fileSession.Session.EndTimestamp < minDate))
					continue;
			}
			
			// Progress reporting
			if(results.allRecords.size() > lastProgress + 40)
			{
				long date = results.allRecords.get(lastProgress + 40).getArg1().getTimestamp();
				
				// check that the records being added now are beyond that of that area
				if(reversed && fileSession.Session.StartTimestamp > date)
				{
					lastProgress = results.allRecords.size();
					reportProgress(results);
				}
				else if(!reversed && fileSession.Session.EndTimestamp < date)
				{
					lastProgress = results.allRecords.size();
					reportProgress(results);
				}
			}
			
			// Load up the records for the session
			RecordList records = fileSession.Log.loadSession(fileSession.Session);
			
			processRecords(records, cause);
		}
		
		LogUtil.info(String.format("Search completed. Logs opened: %d, Sessions Searched: %d, Records found in active buffers: %d, Total Searched Records: %d, Matching Records: %d", sessionsToSearch.getLogCount(), sessionCount, bufferedCount, totalRecords, results.allRecords.size()));
		sessionsToSearch.release();
		
		for(Modifier modifier : mFilter.modifiers)
		{
			if(modifier instanceof FormatterModifier)
				((FormatterModifier)modifier).format(results);
		}
		
		return results;
	}

	@Override
	public int getTaskTargetId() 
	{
		return -1;
	}

	@Override
	public au.com.mineauz.PlayerSpy.LogTasks.Task.Priority getTaskPriority()
	{
		return Priority.High;
	}
}
