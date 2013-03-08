package au.com.mineauz.PlayerSpy.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;
import au.com.mineauz.PlayerSpy.tracdata.LogFileRegistry;

public class SpyCommand implements ICommand 
{

	@Override
	public String getName() 
	{
		return "spy";
	}

	@Override
	public String[] getAliases() 
	{
		return new String[] {"record"};
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
		return "Starts deeply recording a player. Enough information will be recorded to reconstruct their exact movements and interactions. \n" + ChatColor.RED + "WARNING: " + ChatColor.WHITE + "This consumes a LARGE amount of disk space. Use sparingly!";
	}

	@Override
	public boolean canBeConsole() { return true; }
	
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
			if(GlobalMonitor.instance.getDeepMonitor(player) != null)
				sender.sendMessage(ChatColor.RED + player.getName() + " is already being monitored closely");
			else
			{
				GlobalMonitor.instance.attachDeep(player);
				sender.sendMessage(ChatColor.GREEN + player.getName() + " is now being monitored closely");
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
			// Get all the matching players online or not
			String searchTerm = args[0].toLowerCase();

			for(OfflinePlayer player : Bukkit.getOfflinePlayers())
			{
				if(player.getName().toLowerCase().startsWith(searchTerm))
				{
					results.add(player.getName());
				}
			}
		}
		return results;
	}


}
