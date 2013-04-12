package au.com.mineauz.PlayerSpy.commands.playback;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.PlaybackContext;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.debugging.Debug;

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
			Debug.info("Tp request failed. location: %s world: %s", (location != null ? Utility.locationToStringShort(location) : "null"), location != null && location.getWorld() != null ? location.getWorld().getName() : "null");
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
