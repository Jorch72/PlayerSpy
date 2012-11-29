package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.inspect.Inspector;

public class InspectCommand implements ICommand
{
	@Override
	public String getName() 
	{
		return "inspect";
	}

	@Override
	public String[] getAliases() 
	{
		return new String[] { "i" };
	}

	@Override
	public String getPermission() 
	{
		return "playerspy.inspect";
	}

	@Override
	public String getUsageString(String label) 
	{
		return label;
	}

	@Override
	public boolean canBeConsole() {	return false; }

	@Override
	public boolean canBeCommandBlock() { return false; }
	
	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length != 0)
			return false;
		
		Inspector.instance.toggleInspect((Player)sender);
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
