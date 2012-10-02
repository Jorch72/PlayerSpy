package au.com.mineauz.PlayerSpy.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class HistoryCommand implements ICommand
{

	@Override
	public String getName() 
	{
		return "history";
	}

	@Override
	public String[] getAliases() 
	{
		return null;
	}

	@Override
	public String getPermission() 
	{
		return "playerspy.inspect";
	}

	@Override
	public String getUsageString(String label) 
	{
		return label + ChatColor.GREEN + "[other args] [page]";
	}

	@Override
	public boolean canBeConsole() {	return false; }

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		return false;
	}
	
}
