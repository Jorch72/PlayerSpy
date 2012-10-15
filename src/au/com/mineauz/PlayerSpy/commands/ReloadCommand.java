package au.com.mineauz.PlayerSpy.commands;

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
	public String getUsageString( String label )
	{
		return label;
	}

	@Override
	public boolean canBeConsole()
	{
		return true;
	}

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

}
