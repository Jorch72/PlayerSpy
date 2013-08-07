package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.honeypot.HoneypotManager;

public class HoneypotCommand implements ICommand
{
	@Override
	public String getName()
	{
		return "honeypot";
	}

	@Override
	public String[] getAliases()
	{
		return new String[] {"hp"};
	}

	@Override
	public String getPermission()
	{
		return "playerspy.honeypot";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		return new String[] {label};
	}

	@Override
	public String getDescription()
	{
		return "Toggles honeypot mode";
	}

	@Override
	public boolean canBeConsole()
	{
		return false;
	}

	@Override
	public boolean canBeCommandBlock()
	{
		return false;
	}

	@Override
	public boolean onCommand( CommandSender sender, String label, String[] args )
	{
		if(args.length != 0)
			return false;
		
		HoneypotManager.instance.toggle((Player)sender);
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
