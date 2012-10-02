package au.com.mineauz.PlayerSpy.commands.playback;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.PlaybackContext;
import au.com.mineauz.PlayerSpy.commands.Command;

public class TpCommand extends Command
{
	@Override
	public boolean onCommand(Player sender, PlaybackContext playback, String[] args) 
	{
		Location location = playback.getTargetLocation(0);
		if(location != null && location.getWorld() != null)
		{
			sender.teleport(location);
			sender.sendMessage("You have been teleported to the playback's location");
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "Unable to tp. Either nothing is loaded yet, or the world was deleted");
			LogUtil.fine("Tp request failed. either location == null or world == null");
		}
		
		return true;
	}

	@Override
	public String getUsage() 
	{
		return "tp";
	}

	@Override
	public String getDescription() 
	{
		return "Teleports you to the current playback location. Beware that it may be unsafe";
	}

}
