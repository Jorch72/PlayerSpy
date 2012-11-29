package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.SpyPlugin;

public class HelpCommand implements ICommand
{
	@Override
	public String getName() 
	{
		return "help";
	}

	@Override
	public String[] getAliases() 
	{
		return null;
	}

	@Override
	public String getPermission() 
	{
		return "playerspy.help";
	}

	@Override
	public String getUsageString(String label) 
	{
		return label;
	}

	@Override
	public boolean canBeConsole() { return true; }

	@Override
	public boolean canBeCommandBlock() { return false; }
	
	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length != 0)
			return false;
		
		sender.sendMessage(ChatColor.AQUA + SpyPlugin.getInstance().getDescription().getName() + ChatColor.WHITE + " version " + ChatColor.AQUA + SpyPlugin.getInstance().getDescription().getVersion());
		sender.sendMessage(" by Schmoller");
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
