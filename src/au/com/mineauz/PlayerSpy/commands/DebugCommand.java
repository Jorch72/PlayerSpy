package au.com.mineauz.PlayerSpy.commands;

import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.FLLTest;

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
		FLLTest test = new FLLTest();
		test.doTest2();
		
		return true;
	}

}
