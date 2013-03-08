package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.inspect.InspectInfo;
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
	public String[] getUsageString(String label, CommandSender sender) 
	{
		return new String[] {label + ChatColor.GREEN + " [entities] [transactions] [uses] [blocks] [resultCount]"};
	}

	@Override
	public String getDescription()
	{
		return "Toggles inspect with /ps inspect. You can optionally specify exactly what results you wish to see. You can change what options are enabled without leaving inspect.";
	}
	
	@Override
	public boolean canBeConsole() {	return false; }

	@Override
	public boolean canBeCommandBlock() { return false; }
	
	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length > 4)
			return false;
		
		InspectInfo settings = new InspectInfo();
		settings.loadDefaults();
		
		boolean ent = false;
		boolean tra = false;
		boolean use = false;
		boolean res = false;
		boolean block = false;
		
		for (int i = 0; i < args.length; ++i)
		{
			if(args[i].equalsIgnoreCase("entities"))
			{
				if(ent)
				{
					sender.sendMessage(ChatColor.RED + "entities already specified!");
					return true;
				}
				
				if(!ent && !tra && !use && !block)
					settings.showEntities = settings.showItems = settings.showUse = settings.showBlocks = false;
				
				ent = true;
				settings.showEntities = true;
			}
			else if(args[i].equalsIgnoreCase("transactions"))
			{
				if(tra)
				{
					sender.sendMessage(ChatColor.RED + "transactions already specified!");
					return true;
				}
				
				if(!ent && !tra && !use && !block)
					settings.showEntities = settings.showItems = settings.showUse = settings.showBlocks = false;
				
				tra = true;
				settings.showItems = true;
			}
			else if(args[i].equalsIgnoreCase("uses"))
			{
				if(use)
				{
					sender.sendMessage(ChatColor.RED + "uses already specified!");
					return true;
				}
				
				if(!ent && !tra && !use && !block)
					settings.showEntities = settings.showItems = settings.showUse = settings.showBlocks = false;
				
				use = true;
				settings.showUse = true;
			}
			else if(args[i].equalsIgnoreCase("blocks"))
			{
				if(block)
				{
					sender.sendMessage(ChatColor.RED + "blocks already specified!");
					return true;
				}
				
				if(!ent && !tra && !use && !block)
					settings.showEntities = settings.showItems = settings.showUse = settings.showBlocks = false;
				
				block = true;
				settings.showBlocks = true;
			}
			else
			{
				try
				{
					int count = Integer.parseInt(args[i]);
					
					if(res)
					{
						sender.sendMessage(ChatColor.RED + "count already specified!");
						return true;
					}
					
					if(count <= 0)
					{
						sender.sendMessage(ChatColor.RED + "Expected count to be interger greater than 0");
						return true;
					}
					if(count > 50)
					{
						sender.sendMessage(ChatColor.RED + "You think you will be able to see " + count + " items at once?");
						return true;
					}
					
					settings.itemCount = count;
					res = true;
				}
				catch(NumberFormatException e)
				{
					sender.sendMessage(ChatColor.RED + "Unknown argument " + args[i]);
					return true;
				}
			}
		}
		
		if(Inspector.instance.isInspecting((Player)sender))
		{
			if(ent || tra || use || res || block)
				Inspector.instance.updateInspect((Player)sender, settings);
			else
				Inspector.instance.disableInspect((Player)sender);
		}
		else
			Inspector.instance.enableInspect((Player)sender, settings);
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
