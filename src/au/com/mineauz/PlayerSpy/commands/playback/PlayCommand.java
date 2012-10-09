package au.com.mineauz.PlayerSpy.commands.playback;

import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.PlaybackContext;

public class PlayCommand extends Command
{

	@Override
	public boolean onCommand(Player sender, PlaybackContext playback, String[] args) {
		if(args.length == 0)
		{
			playback.play();
			return true;
		}
		return false;
	}

	@Override
	public String getUsage() 
	{
		return "play";
	}

	@Override
	public String getDescription() 
	{
		return "Plays or resumes playback";
	}
	
}
