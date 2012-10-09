package au.com.mineauz.PlayerSpy.commands.playback;

import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.PlaybackContext;

public class PauseCommand extends Command
{

	@Override
	public boolean onCommand(Player sender, PlaybackContext playback, String[] args) 
	{
		if(args.length == 0)
		{
			playback.pause();
			return true;
		}
		return false;
	}

	@Override
	public String getUsage() 
	{
		return "pause";
	}

	@Override
	public String getDescription() 
	{
		return "Pauses playback";
	}
	
}
