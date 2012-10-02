package au.com.mineauz.PlayerSpy.commands.playback;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.PlaybackContext;
import au.com.mineauz.PlayerSpy.commands.Command;

public class HelpCommand extends Command 
{
	private Map<String,Command> mCommandMap;
	public HelpCommand(Map<String,Command> commandMap)
	{
		mCommandMap = commandMap;
	}
	@Override
	public boolean onCommand(Player sender, PlaybackContext playback, String[] args) 
	{
		if(args.length == 0)
		{
			// Build the list
			String output = ChatColor.AQUA + "Playback Help:" + ChatColor.GRAY + "\n";
			output += "Use help <command> to find out more information" + ChatColor.WHITE + "\n";
			
			for(Map.Entry<String,Command> entry : mCommandMap.entrySet())
			{
				output += entry.getValue().getUsage() + "\n";
			}
			
			sender.sendMessage(output);
		}
		else
		{
			if(mCommandMap.containsKey(args[0].toLowerCase()))
			{
				String output = mCommandMap.get(args[0].toLowerCase()).getUsage() + ChatColor.GRAY + "\n" +
						mCommandMap.get(args[0].toLowerCase()).getDescription();
				
				sender.sendMessage(output);
			}
			else
				sender.sendMessage("There is no command '" + args[0] + "'");
		}
		return true;
	}
	@Override
	public String getUsage() 
	{
		return "help [command]";
	}
	@Override
	public String getDescription() 
	{
		return "Shows all available commands or displays detailed information about a command";
	}

}
