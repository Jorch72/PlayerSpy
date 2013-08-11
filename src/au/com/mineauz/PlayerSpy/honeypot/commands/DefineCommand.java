package au.com.mineauz.PlayerSpy.honeypot.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.commands.ICommand;
import au.com.mineauz.PlayerSpy.honeypot.HoneypotManager;
import au.com.mineauz.PlayerSpy.honeypot.Honeypot;
import au.com.mineauz.PlayerSpy.honeypot.HoneypotManager.Selection;

public class DefineCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "define";
	}

	@Override
	public String[] getAliases()
	{
		return null;
	}

	@Override
	public String getPermission()
	{
		return "playerspy.honeypot";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		return new String[] {label + ChatColor.GREEN + " [<points>]"};
	}

	@Override
	public String getDescription()
	{
		return "Defines a honey pot";
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
		if(args.length > 1)
			return false;
		
		Selection selection = HoneypotManager.instance.getSelection((Player)sender);
		
		if(selection == null)
		{
			sender.sendMessage(ChatColor.RED + "Cannot create a honeypot, nothing is selected.");
			return true;
		}

		int points = 1;
		if(args.length == 1)
		{
			try
			{
				points = Integer.parseInt(args[0]);
				
				if(points <= 0)
				{
					sender.sendMessage(ChatColor.RED + "Expected a positive integer for number of points");
					return true;
				}
			}
			catch(NumberFormatException e)
			{
				sender.sendMessage(ChatColor.RED + "Expected a positive integer for number of points");
				return true;
			}
		}
		
		Honeypot honeypot = new Honeypot();
		honeypot.points = points;
		honeypot.region = selection.clone();

		HoneypotManager.instance.addHoneypot(honeypot);
		
		if(selection.getBlockCount() == 1)
		{
			Block block = selection.getFirst().getBlock();
			String name = Utility.formatItemName(new ItemStack(block.getType(), 1, block.getData()));
			sender.sendMessage("Honeypot created for " + ChatColor.DARK_AQUA + name + ChatColor.WHITE + " at " + ChatColor.GREEN + Utility.locationToStringShorter(block.getLocation()));
		}
		else
		{
			int blocks = selection.getBlockCount();
			sender.sendMessage("Honeypot created from " + ChatColor.GREEN + Utility.locationToStringShorter(selection.getFirst()) + ChatColor.WHITE + " to " + ChatColor.GREEN + Utility.locationToStringShorter(selection.getSecond()) + ChatColor.DARK_AQUA + " " + blocks + ChatColor.WHITE + " blocks total");
		}
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
