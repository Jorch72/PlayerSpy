package au.com.mineauz.PlayerSpy.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;

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
		return null;
	}

	@Override
	public String getPermission() 
	{
		return "playerspy.spy";
	}

	@Override
	public String getUsageString(String label) 
	{
		return label + ChatColor.GOLD + " <player>";
	}

	@Override
	public boolean canBeConsole() {	return true; }

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

}
