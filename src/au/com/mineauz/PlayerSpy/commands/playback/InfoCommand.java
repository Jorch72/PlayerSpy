package au.com.mineauz.PlayerSpy.commands.playback;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.PlaybackContext;
import au.com.mineauz.PlayerSpy.Util;
import au.com.mineauz.PlayerSpy.commands.Command;

public class InfoCommand extends Command
{
	@Override
	public boolean onCommand(Player sender, PlaybackContext playback, String[] args) 
	{
		if(args.length == 0)
		{
			sender.sendMessage("Target Name: " + ChatColor.GREEN + playback.getTargetName(0) + ChatColor.WHITE + "\n"
					+ "Earliest Date: " + ChatColor.GREEN + Util.dateToString(playback.getStartDate()) + ChatColor.WHITE + "\n"
					+ "Latest Date: " + ChatColor.GREEN + Util.dateToString(playback.getEndDate()) + ChatColor.WHITE + "\n");
		}
		else
			return false;
		
		return true;
	}

	@Override
	public String getUsage() 
	{
		return "info";
	}

	@Override
	public String getDescription() 
	{
		return "Gets basic information about the currently loaded log";
	}
	
}
