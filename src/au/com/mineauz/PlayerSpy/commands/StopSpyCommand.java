package au.com.mineauz.PlayerSpy.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;
import au.com.mineauz.PlayerSpy.tracdata.LogFileRegistry;

public class StopSpyCommand implements ICommand
{
	@Override
	public String getName() 
	{
		return "stopspy";
	}

	@Override
	public String[] getAliases() 
	{
		return new String[] { "stop" };
	}

	@Override
	public String getPermission() 
	{
		return "playerspy.spy";
	}

	@Override
	public String[] getUsageString(String label, CommandSender sender) 
	{
		return new String[] {label + ChatColor.GOLD + " <player>"};
	}
	
	@Override
	public String getDescription()
	{
		return "Stops deeply recording a player.";
	}

	@Override
	public boolean canBeConsole() {	return true; }
	
	@Override
	public boolean canBeCommandBlock() { return false; }

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length != 1)
			return false;
		
		OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
		if(player == null)
		{
			sender.sendMessage(ChatColor.RED + "Unknown player: " + args[0]);
		}
		else
		{
			if(GlobalMonitor.instance.getDeepMonitor(player) == null)
				sender.sendMessage(ChatColor.RED + player.getName() + " is not being closely monitored currently");
			else
			{
				GlobalMonitor.instance.removeDeep(player);
				sender.sendMessage(ChatColor.GREEN + player.getName() + " is no longer being closely monitored");
			}
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		ArrayList<String> results = new ArrayList<String>();
		
		if(args.length == 0)
		{
			// Get the first player that has a log file

			for(OfflinePlayer player : Bukkit.getOfflinePlayers())
			{
				if(LogFileRegistry.hasLogFile(player))
				{
					results.add(player.getName());
					break;
				}
			}
		}
		else if(args.length == 1)
		{
			// Get all the matching players that have log files
			String searchTerm = args[0].toLowerCase();

			for(OfflinePlayer player : Bukkit.getOfflinePlayers())
			{
				if(player.getName().toLowerCase().startsWith(searchTerm))
				{
					if(LogFileRegistry.hasLogFile(player))
						results.add(player.getName());
				}
			}
		}
		return results;
	}

}
