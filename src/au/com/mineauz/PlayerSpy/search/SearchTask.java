package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.Pair;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.LogTasks.Task;
import au.com.mineauz.PlayerSpy.Records.AttackRecord;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.ChatCommandRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex.SessionInFile;

public class SearchTask implements Task<SearchResults>
{
	private SearchFilter mFilter;
	private Location mLocation;
	public SearchTask(SearchFilter filter, Location location)
	{
		mFilter = filter;
		if(location != null)
			mLocation = location.clone();
	}
	@SuppressWarnings("deprecation")
	@Override
	public SearchResults call() throws Exception 
	{
		List<SessionInFile> sessionsToSearch = null;
		
		// Get the contraints
		long startTime = 0, endTime = Long.MAX_VALUE;
		int distance = -1;
		ArrayList<OfflinePlayer> playerConstraints = new ArrayList<OfflinePlayer>();
		
		for(Constraint constraint : mFilter.constraints)
		{
			if(constraint instanceof DateConstraint)
			{
				startTime = ((DateConstraint)constraint).startDate.getTime();
				endTime = ((DateConstraint)constraint).endDate.getTime();
			}
			else if(constraint instanceof PlayerConstraint)
			{
				playerConstraints.add(((PlayerConstraint)constraint).player);
			}
			else if(constraint instanceof DistanceConstraint)
			{
				distance = ((DistanceConstraint)constraint).distance;
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
			if(playerConstraints.size() != 0)
			{
				if(!cause.isPlayer())
					continue;
				
				boolean constraintOk = false;
				for(OfflinePlayer player : playerConstraints)
				{
					if(cause.getCausingPlayer().equals(player))
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
			boolean newCause = false;
			boolean addedRecords = false;
			if(reverseCauseMap.containsKey(cause))
				id = reverseCauseMap.get(cause);
			else
			{
				id = nextCauseId++;
				reverseCauseMap.put(cause, id);
				newCause = true;
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
				
				if(mFilter.action instanceof ChatCommandAction)
				{
					if(record.getType() != RecordType.ChatCommand)
						continue;
					
					if(((ChatCommandAction)mFilter.action).command)
					{
						if(!((ChatCommandRecord)record).getMessage().startsWith("/"))
							continue;
					}
					else
					{
						if(((ChatCommandRecord)record).getMessage().startsWith("/"))
							continue;
					}
				}
				else if(mFilter.action instanceof BlockAction)
				{
					if(record.getType() != RecordType.BlockChange)
						continue;
					
					// Check the placed flag
					if(((BlockChangeRecord)record).wasPlaced() != ((BlockAction)mFilter.action).placed)
						continue;
					
					// Check the material
					if(((BlockAction)mFilter.action).material.getArg1() != Material.AIR && ((BlockAction)mFilter.action).material.getArg1() != ((BlockChangeRecord)record).getBlock().getType())
						continue;
					
					// Check the metadata
					if(((BlockAction)mFilter.action).material.getArg2() != -1 && ((BlockAction)mFilter.action).material.getArg2() != ((BlockChangeRecord)record).getBlock().getData())
						continue;
					
					// Check the distance constraint
					if(distance != -1 && mLocation != null)
					{
						if(!((BlockChangeRecord)record).getLocation().getWorld().equals(mLocation.getWorld()))
							continue;
						
						if(((BlockChangeRecord)record).getLocation().distance(mLocation) > distance)
							continue;
					}
				}
				else if(mFilter.action instanceof EntityAction)
				{
					if(((EntityAction)mFilter.action).spawn)
					{
						continue; // No records for this yet
					}
					else
					{
						if(record.getType() != RecordType.Attack || ((AttackRecord)record).getDamage() != -1)
							continue;
						
						if(((EntityAction)mFilter.action).entityType != ((AttackRecord)record).getDamagee().getEntityType())
							continue;
						
						if(((EntityAction)mFilter.action).entityType == EntityType.PLAYER && !((EntityAction)mFilter.action).player.getName().equalsIgnoreCase(((AttackRecord)record).getDamagee().getPlayerName()))
							continue;
					}
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
