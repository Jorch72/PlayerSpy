package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.LogTasks.CatchupTask;
import au.com.mineauz.PlayerSpy.Utilities.Util;

public class CatchupCommand implements ICommand
{
	@Override
	public String getName()
	{
		return "catchup";
	}

	@Override
	public String[] getAliases()
	{
		return null;
	}

	@Override
	public String getPermission()
	{
		return "playerspy.catchup";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		return new String[] {label};
	}

	@Override
	public String getDescription()
	{
		return "Displays up to the last " + Util.dateDiffToString(SpyPlugin.getSettings().catchupTime, false) + " of chat";
	}

	@Override
	public boolean canBeConsole()
	{
		return true;
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
		
		CatchupTask task = new CatchupTask(sender);
		
		SpyPlugin.getExecutor().submit(task);
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
