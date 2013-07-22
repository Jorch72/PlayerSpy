package test.au.com.mineauz.PlayerSpy.runtime;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.bukkit.command.CommandSender;

import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.commands.ICommand;

public class TestCommand implements ICommand
{
	@Override
	public String getName()
	{
		return "test";
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
	public String[] getUsageString( String label, CommandSender sender )
	{
		return new String[] {label};
	}

	@Override
	public String getDescription()
	{
		return "Starts the runtime test suit";
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
