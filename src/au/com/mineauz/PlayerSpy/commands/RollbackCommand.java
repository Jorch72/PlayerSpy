package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

public class RollbackCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "rollback";
	}

	@Override
	public String[] getAliases()
	{
		return new String[] {"rb"};
	}

	@Override
	public String getPermission()
	{
		return "playerspy.rollback";
	}

	@Override
	public String getUsageString( String label )
	{
		return label + " [c:<cause>] [r:<radius>] [t:<type>]";
	}

	@Override
	public boolean canBeConsole()
	{
		return false;
	}

	@Override
	public boolean onCommand( CommandSender sender, String label, String[] args )
	{
		
		return false;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
