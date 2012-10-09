package au.com.mineauz.PlayerSpy.commands.playback;

import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.PlaybackContext;

public abstract class Command 
{
	public abstract boolean onCommand(Player sender, PlaybackContext playback, String[] args);
	
	public abstract String getUsage();
	public abstract String getDescription();
}
