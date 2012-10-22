package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.ACIDTest;


public class DebugCommand implements ICommand
{

	@Override
	public String getName() 
	{
		return "debug";
	}

	@Override
	public String[] getAliases() 
	{
		return null;
	}

	@Override
	public String getPermission() 
	{
		return null;
	}

	@Override
	public String getUsageString(String label) 
	{
		return label;
	}

	@Override
	public boolean canBeConsole() 
	{
		return true;
	}

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length != 1)
			return false;
		
		ACIDTest test = new ACIDTest();
		if(args[0].equals("1"))
			test.test1();
		if(args[0].equals("2"))
			test.test2();
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
