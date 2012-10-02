package au.com.mineauz.PlayerSpy.commands.playback;

import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.PlaybackContext;
import au.com.mineauz.PlayerSpy.Util;
import au.com.mineauz.PlayerSpy.commands.Command;

public class SkipCommand extends Command
{

	@Override
	public boolean onCommand(Player sender, PlaybackContext playback, String[] args) 
	{
		if(args.length == 0)
		{
			playback.skip(0);
		}
		else
		{
			String dateDiffString = args[0];
			for(int i = 1; i < args.length; i++)
				dateDiffString += " " + args[i];
			
			long time = Util.parseDateDiff(dateDiffString);

			if(time != 0)
				playback.skip(time);
			else
				sender.sendMessage("Incorrect time difference format. Should be like: 1m 10s");
		}
		return true;
	}

	@Override
	public String getUsage() 
	{
		return "skip [dateDiff]";
	}

	@Override
	public String getDescription() 
	{
		return "If no date difference is specified, it moves playback to the next available record. Otherwise it moves playback by the amount specified";
	}
	
}
