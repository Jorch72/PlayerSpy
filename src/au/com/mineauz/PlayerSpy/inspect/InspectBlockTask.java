package au.com.mineauz.PlayerSpy.inspect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Pair;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Util;
import au.com.mineauz.PlayerSpy.Utility;
import au.com.mineauz.PlayerSpy.LogTasks.BlockHistoryTask;
import au.com.mineauz.PlayerSpy.LogTasks.Task;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.Record;

public class InspectBlockTask implements Task<Void>
{
	private Player mWho;
	private Location mLocation;
	
	public InspectBlockTask(Player who, Location block)
	{
		mWho = who;
		mLocation = block.clone();
	}
	@Override
	public Void call() 
	{
		BlockHistoryTask task = new BlockHistoryTask(mLocation);
		HashMap<String, RecordList> results = null;
		try
		{
			results = task.call();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
		// Find the 3 Most recent results
		int maxResults = 3;
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
				if(!added && mostRecent.size() < maxResults)
					mostRecent.add(new Pair<String, Record>(ent.getKey(), record));
				else if(added && mostRecent.size() > maxResults)
					mostRecent.remove(mostRecent.size()-1);
			}
		}
		
		// Format the results into a neat list and display
		ArrayList<String> output = new ArrayList<String>();
		
		output.add(ChatColor.GOLD + "[PlayerSpy] " + ChatColor.WHITE + "Inspect block changes @" + Utility.locationToStringShort(mLocation));
		if(mostRecent.size() == 0)
			output.add(ChatColor.GREEN + "  No changes to the block detected");
		else
		{
			for(int i = 0; i < mostRecent.size(); i++)
			{
				BlockChangeRecord record = (BlockChangeRecord)mostRecent.get(i).getArg2();
				
				String blockName = Utility.formatItemName(new ItemStack(record.getBlock().getType(),1, record.getBlock().getData()));
				output.add("  " + ChatColor.GREEN + Util.dateToString(record.getTimestamp()) + ChatColor.WHITE + ": " + blockName + " " + (record.wasPlaced() ? "placed by " : "broken by ") + ChatColor.DARK_AQUA + mostRecent.get(i).getArg1());
			}
		}
		
		// Send the message
		for(String line : output)
		{
			mWho.sendMessage(line);
		}
		return null;
	}
	@Override
	public int getTaskTargetId() 
	{
		return 99999999;
	}

}
