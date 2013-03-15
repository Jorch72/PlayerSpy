package au.com.mineauz.PlayerSpy.LogTasks;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.ChatCommandRecord;
import au.com.mineauz.PlayerSpy.search.ChatConstraint;
import au.com.mineauz.PlayerSpy.search.ReverserFormatter;
import au.com.mineauz.PlayerSpy.search.SearchFilter;
import au.com.mineauz.PlayerSpy.search.SearchResults;
import au.com.mineauz.PlayerSpy.search.SearchTask;
import au.com.mineauz.PlayerSpy.search.TimeConstraint;

public class CatchupTask implements Task<Void>
{
	private final CommandSender mSender;
	private final String mFormat;
	private final long mTime;
	public CatchupTask(CommandSender sender)
	{
		mSender = sender;
		mFormat = SpyPlugin.getSettings().chatFormat;
		mTime = SpyPlugin.getSettings().catchupTime;
		
	}

	@Override
	public Void call() throws Exception
	{
		SearchFilter filter = new SearchFilter();
		filter.andConstraints.add(new ChatConstraint());
		filter.andConstraints.add(new TimeConstraint(System.currentTimeMillis() - mTime, true));
		
		filter.modifiers.add(new ReverserFormatter());
		
		SearchTask task = new SearchTask(filter);
		SearchResults results = task.call();
		
		// Display the results
		
		int start = 0;
		if(results.allRecords.size() > 80)
			start = results.allRecords.size()-80-1;
		
		for(int i = start; i < results.allRecords.size(); ++i)
		{
			if(results.allRecords.get(i).getArg1() instanceof ChatCommandRecord)
			{
				ChatCommandRecord record = (ChatCommandRecord)results.allRecords.get(i).getArg1();
				Cause cause = results.causes.get(results.allRecords.get(i).getArg2());
				
				if(!cause.isPlayer())
					continue;
				
				String displayMsg = "";
				if(cause.getCausingPlayer().isOnline())
					displayMsg = cause.getCausingPlayer().getPlayer().getDisplayName();
				else
					displayMsg = cause.getCausingPlayer().getName();
				
				
				displayMsg = ChatColor.translateAlternateColorCodes('&', mFormat.replaceAll("%name", displayMsg).replaceAll("%message", record.getMessage()));
				mSender.sendMessage(displayMsg);
			}
		}
		
		return null;
	}

	@Override
	public int getTaskTargetId()
	{
		return -1;
	}

}
