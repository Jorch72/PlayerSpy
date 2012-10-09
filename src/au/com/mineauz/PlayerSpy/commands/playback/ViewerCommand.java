package au.com.mineauz.PlayerSpy.commands.playback;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.PlaybackContext;
import au.com.mineauz.PlayerSpy.SpyPlugin;

public class ViewerCommand extends Command
{

	@Override
	public boolean onCommand(Player sender, PlaybackContext playback, String[] args) 
	{
		if(args.length != 2)
			return false;
		
		boolean add = false;
		if(args[0].compareToIgnoreCase("add") == 0)
			add = true;
		else if(args[0].compareToIgnoreCase("remove") != 0)
		{
			sender.sendMessage(ChatColor.RED + "Invalid Arg. Expected 'add' or 'remove' for first argument");
			return false;
		}
		
		Player player = SpyPlugin.getInstance().getServer().getPlayer(args[1]);
		if(player == null)
		{
			sender.sendMessage(ChatColor.RED + "Invalid Arg. Expected player name of online player for second argument");
			return false;
		}
		
		if(add)
		{
			playback.addViewer(player);
			sender.sendMessage(player.getName() + " is now able to see this playback");
		}
		else
		{
			playback.removeViewer(player);
			sender.sendMessage(player.getName() + " is nolonger able to see this playback");
		}
		
		return true;
	}

	@Override
	public String getUsage() 
	{
		return "viewer (add|remove) <player>";
	}

	@Override
	public String getDescription() 
	{
		return "Adds or removes a viewer to/from this playback.";
	}

}
