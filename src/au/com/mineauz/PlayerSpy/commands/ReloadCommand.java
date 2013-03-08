package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.SpyPlugin;

public class ReloadCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "reload";
	}

	@Override
	public String[] getAliases()
	{
		return null;
	}

	@Override
	public String getPermission()
	{
		return "playerspy.reload";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		return new String[] {label};
	}

	@Override
	public String getDescription()
	{
		return "Reloads the playerspy configuration.";
	}
	
	@Override
	public boolean canBeConsole() { return true; }
	
	@Override
	public boolean canBeCommandBlock() { return false; }

	@Override
	public boolean onCommand( CommandSender sender, String label, String[] args )
	{
		if(args.length != 0)
			return false;
		
		SpyPlugin.getSettings().load();
		SpyPlugin.getSettings().save();
		
		sender.sendMessage(ChatColor.GREEN + "PlayerSpy configuration reloaded.");
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
