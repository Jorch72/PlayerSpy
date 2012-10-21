package au.com.mineauz.PlayerSpy.commands.playback;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.PlaybackContext;
import au.com.mineauz.PlayerSpy.Util;
import au.com.mineauz.PlayerSpy.Utility;

public class SeekCommand extends Command
{
	@Override
	public boolean onCommand(Player sender, PlaybackContext playback, String[] args) 
	{
		if(args.length == 0)
		{
			sender.sendMessage("Playback is at " + ChatColor.GREEN + Utility.formatTime(playback.getPlaybackDate(), "dd/MM/yy HH:mm:ss"));
		}
		else
		{
			String dateString = args[0];
			for(int i = 1; i < args.length; i++)
				dateString += " " + args[i];
			long time = Util.parseDate(dateString, playback.getPlaybackDate(), playback.getStartDate(), playback.getEndDate());
			
			if(time != 0)
			{
				if(!playback.seek(time))
				{
					sender.sendMessage("There is no session at " + ChatColor.GREEN + Utility.formatTime(time, "dd/MM/yy HH:mm:ss"));
				}
			}
			else
				sender.sendMessage("Incorrect date format.");
		}
		return true;
	}

	@Override
	public String getUsage() 
	{
		return "seek [date]";
	}

	@Override
	public String getDescription() 
	{
		return "If no date is specified, it displays the current playback date. Otherwise, it seeks to the date specified. It fails if there is no session at that date. In that case use 'prev' or 'next' with that date to get the session near it.";
	}
	
}
