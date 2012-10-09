package au.com.mineauz.PlayerSpy.search;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avaje.ebeaninternal.api.BindParams.OrderedList;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.Pager;
import au.com.mineauz.PlayerSpy.Pair;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Util;
import au.com.mineauz.PlayerSpy.Utility;
import au.com.mineauz.PlayerSpy.Records.AttackRecord;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.ChatCommandRecord;
import au.com.mineauz.PlayerSpy.Records.Record;

public class Searcher 
{
	public static final Searcher instance = new Searcher();
	
	private HashMap<CommandSender, Future<SearchResults>> mWaitingTasks;
	private HashMap<CommandSender, SearchResults> mCachedResults;
	
	private Searcher() 
	{
		mWaitingTasks = new HashMap<CommandSender, Future<SearchResults>>();
		mCachedResults = new HashMap<CommandSender, SearchResults>();
	}
	
	public void update()
	{
		Iterator<Entry<CommandSender, Future<SearchResults>>> it = mWaitingTasks.entrySet().iterator();
		while(it.hasNext())
		{
			Entry<CommandSender, Future<SearchResults>> entry = it.next();
			
			if(entry.getValue().isDone())
			{
				try 
				{
					mCachedResults.put(entry.getKey(), entry.getValue().get());
					displayResults(entry.getKey(), 0);
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				} 
				catch (ExecutionException e) 
				{
					e.printStackTrace();
				}
				it.remove();
			}
		}
	}
	
	public void searchAndDisplay(CommandSender sender, SearchFilter filter)
	{
		SearchTask task = new SearchTask(filter, (sender instanceof Player ? ((Player)sender).getLocation() : null));
		mWaitingTasks.put(sender, SpyPlugin.getExecutor().submit(task));
	}
	
	public void displayResults(CommandSender who, int page)
	{
		if(!mCachedResults.containsKey(who))
			return;
		
		if(who instanceof Player && !(((Player)who).isOnline()))
		{
			mCachedResults.remove(who);
			return;
		}
		
		SearchResults results = mCachedResults.get(who);
		Pager pager = new Pager("Search results", (who instanceof Player ? 10 : 40));
		for(Pair<Record,Integer> result : results.allRecords)
		{
			Cause cause = results.causes.get(result.getArg2());
			String msg = "record";
			switch(result.getArg1().getType())
			{
			case Attack:
				if(((AttackRecord)result.getArg1()).getDamagee().getEntityType() == EntityType.PLAYER)
				{
					msg = ((AttackRecord)result.getArg1()).getDamagee().getPlayerName() + " was killed @" + Utility.locationToStringShort(((AttackRecord)result.getArg1()).getDamagee().getLocation());
				}
				else
				{
					msg = ((AttackRecord)result.getArg1()).getDamagee().getEntityType().getName() + " was killed @" + Utility.locationToStringShort(((AttackRecord)result.getArg1()).getDamagee().getLocation());
				}
				break;
			case BlockChange:
				msg = Utility.formatItemName(new ItemStack(((BlockChangeRecord)result.getArg1()).getBlock().getType()));
				if(((BlockChangeRecord)result.getArg1()).wasPlaced())
					msg += " placed ";
				else
					msg += " removed ";
				
				break;
			case ChatCommand:
				msg = "'" + ((ChatCommandRecord)result.getArg1()).getMessage() + "'";
				break;
			default:
				break;
			}
			pager.addItem(String.format(ChatColor.GREEN + "%16s" + ChatColor.RESET + " %s by " + ChatColor.DARK_AQUA + "%s", Util.dateToString(result.getArg1().getTimestamp()), msg, (cause.isGlobal() ? cause.getExtraCause() : (cause.isPlayer() ? Utility.formatName(cause.getCausingPlayer().getName(), cause.getExtraCause()) : "Unknown"))));
		}
		
		pager.displayPage(who, page);
		who.sendMessage(ChatColor.GOLD + "Use '/ps page " + (page + 2) + "' to view the next page");
		
	}
	
}
