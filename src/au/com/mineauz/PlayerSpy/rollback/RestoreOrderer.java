package au.com.mineauz.PlayerSpy.rollback;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;

import com.google.common.collect.HashMultimap;

import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.search.SearchResults;
import au.com.mineauz.PlayerSpy.search.interfaces.FormatterModifier;

public class RestoreOrderer implements FormatterModifier
{
	private boolean mForward = false;
	private HashMap<Location, Long> mBlockLocations = new HashMap<Location, Long>();
	private HashMultimap<Location, Pair<Record, Integer>> mDependantMap;
	
	public RestoreOrderer(boolean forward)
	{
		mForward = forward;
		mDependantMap = HashMultimap.create();
	}
	private boolean processRecord(Record record, int causeId)
	{
		if(!(record instanceof BlockChangeRecord))
			return true;
		
		BlockChangeRecord brecord = (BlockChangeRecord)record;
		
		Long time = mBlockLocations.get(brecord.getLocation());
		
		if(time != null)
		{
			if( mForward && brecord.getTimestamp() <= time )
				return false;
			if( !mForward && brecord.getTimestamp() >= time )
				return false;
		}
		
		Location depend = Utility.getDependantLocation(mForward ? brecord.getFinalBlock() : brecord.getInitialBlock());
		
		mBlockLocations.put(brecord.getLocation(), brecord.getTimestamp());
		
		if(depend != null)
		{
			mDependantMap.put(depend, new Pair<Record, Integer>(record, causeId));
			
			return false;
		}
		
		return true;
	}
	
	@Override
	public void format( SearchResults results )
	{
		HashSet<Pair<Record, Integer>> toRemove = new HashSet<Pair<Record,Integer>>();
		
		// Stage 1: Screen out any intermediate block changes and find dependencies
		for(int i = 0; i < results.allRecords.size(); ++i)
		{
			Record record = results.allRecords.get(i).getArg1();
			
			if(!processRecord(record, results.allRecords.get(i).getArg2()))
				toRemove.add(results.allRecords.get(i));
		}
		
		results.allRecords.removeAll(toRemove);
		
		// Stage 2: Sort the records
		Collections.sort(results.allRecords, new Comparator<Pair<Record, Integer>>()
		{
			@Override
			public int compare( Pair<Record, Integer> o1, Pair<Record, Integer> o2 )
			{
				BlockChangeRecord a = null,b = null;
				
				if(o1.getArg1() instanceof BlockChangeRecord)
					a = (BlockChangeRecord)o1.getArg1();
				
				if(o2.getArg1() instanceof BlockChangeRecord)
					b = (BlockChangeRecord)o2.getArg1();
				
				// All non block change records go to the end
				if(a == null && b != null)
					return 1;
				if(a != null && b == null)
					return -1;
				if(a == null && b == null)
					return 0;
				
				Location la = a.getLocation();
				Location lb = b.getLocation();
				
				int score = 0;
				
				score = Integer.compare(la.getBlockX(), lb.getBlockX());
				if(score != 0)
					return score;
				
				score = Integer.compare(la.getBlockY(), lb.getBlockY());
				if(score != 0)
					return score;
				
				score = Integer.compare(la.getBlockZ(), lb.getBlockZ());
				
				return score;
			}
		}); 
		
		// Stage 3: Re-insert the dependent records into the correct position
		for(int i = 0; i < results.allRecords.size(); ++i)
		{
			Record record = results.allRecords.get(i).getArg1();
			
			if(!(record instanceof BlockChangeRecord))
				continue;
			
			BlockChangeRecord brecord = (BlockChangeRecord)record;
			
			Set<Pair<Record,Integer>> dependancies = mDependantMap.get(brecord.getLocation());
			if(dependancies != null)
			{
				for(Pair<Record,Integer> pair : dependancies)
					results.allRecords.add(i+1, pair);
			}
		}
		
	}

}
