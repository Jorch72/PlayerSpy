package au.com.mineauz.PlayerSpy.commands;

import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.Utilities.Util;
import au.com.mineauz.PlayerSpy.rollback.RollbackManager;
import au.com.mineauz.PlayerSpy.rollback.RollbackSession;
import au.com.mineauz.PlayerSpy.search.DateConstraint;
import au.com.mineauz.PlayerSpy.search.DistanceConstraint;
import au.com.mineauz.PlayerSpy.search.EndResultOnlyModifier;
import au.com.mineauz.PlayerSpy.search.SearchFilter;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class RollbackCommand implements ICommand
{
	//private static String[] sTypes = {"block change","block place","block break","painting change", "painting place", "painting break", "itemframe change", "itemframe place", "itemframe break", "any"};
	
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
		return label + " [undo|[c:<cause>] [r:<radius>] [t:<time>]]";
	}

	@Override
	public boolean canBeConsole() { return true; }

	@Override
	public boolean canBeCommandBlock() { return false; }
	
	@Override
	public boolean onCommand( CommandSender sender, String label, String[] args )
	{
		if(args.length == 1 && args[0].equalsIgnoreCase("undo"))
		{
			Player who = null;
			if(sender instanceof Player)
				who = (Player)sender;
			
			RollbackSession session = RollbackManager.instance.getLastRollbackSessionFor(who);
			if(session == null)
			{
				sender.sendMessage(ChatColor.RED + "There is nothing to undo");
				return true;
			}
			RollbackManager.instance.undoRollback(session, who);
			return true;
		}
		SearchFilter filter = new SearchFilter();
		filter.modifiers.add(new EndResultOnlyModifier());
		filter.noLimit = true;
		boolean preview = false;
		
		for(int i = 0; i < args.length; i++)
		{
			if(!args[i].contains(":"))
			{
				sender.sendMessage(ChatColor.RED + "Unknown argument: " + args[i]);
				return false;
			}
			
			String c = args[i].split(":")[0].toLowerCase();
			String val = args[i].split(":")[1];
			
			if(c.equals("c")) // Cause
			{
				Cause cause = null;
				if(val.contains(">"))
				{
					String name, extra;
					name = val.substring(0,val.indexOf(">"));
					extra = val.substring(val.indexOf(">") + 1);
					
					OfflinePlayer player = Bukkit.getOfflinePlayer(name);
					if(player.hasPlayedBefore())
						cause= Cause.playerCause(player, extra);
				}
				else if(val.startsWith("#"))
				{
					cause = Cause.globalCause(Bukkit.getWorlds().get(0), val);
				}
				else
				{
					OfflinePlayer player = Bukkit.getOfflinePlayer(val);
					if(player.hasPlayedBefore())
						cause = Cause.playerCause(player);
				}
				
				if(cause == null)
				{
					sender.sendMessage(ChatColor.RED + "Unknown cause: " + val);
					return true;
				}
				
				filter.causes.add(cause);
			}
			else if(c.equals("r")) // Radius
			{
				for(Constraint constraint : filter.andConstraints)
				{
					if(constraint instanceof DistanceConstraint)
					{
						sender.sendMessage(ChatColor.RED + "Radius already specified.");
						return true;
					}
				}
				
				if(!(sender instanceof Player))
				{
					sender.sendMessage(ChatColor.RED + "Cannot use radius if you are not in game");
					return true;
				}
				
				
				int radius = -1;
				try
				{
					radius = Integer.parseInt(val);
					
					if(radius < 0)
					{
						sender.sendMessage(ChatColor.RED + "Expected positive integer for radius, got: " + val);
						return true;
					}
				}
				catch(NumberFormatException e)
				{
					sender.sendMessage(ChatColor.RED + "Expected integer for radius, got: " + val);
					return true;
				}
				
				DistanceConstraint constraint = new DistanceConstraint(radius, ((Player)sender).getLocation());
				filter.andConstraints.add(constraint);
			}
			else if(c.equals("t")) // time
			{
				String timeString = val;
				for(int j = i; j < args.length; j++)
				{
					if(!args[j].contains(":"))
					{
						timeString += " " + args[j];
						i = j;
					}
					else
						break;
				}
				
				long time = Util.parseDateDiff(timeString);
				
				if (time <= 0)
				{
					sender.sendMessage(ChatColor.RED + "Invalid date diff format");
					return true;
				}
				
				DateConstraint constraint = new DateConstraint();
				constraint.startDate = new Date(System.currentTimeMillis() - time);
				constraint.endDate = new Date(System.currentTimeMillis());
				filter.andConstraints.add(constraint);
			}
			else if(c.equals("preview"))
			{
				if(!(sender instanceof Player))
				{
					sender.sendMessage(ChatColor.RED + "Cannot use preview if you are not in game");
					return true;
				}
				preview = true;
			}
		}
		
		if(!(sender instanceof Player))
		{
			RollbackManager.instance.startRollback(filter);
		}
		else
		{
			RollbackManager.instance.startRollback(filter, (Player)sender, preview);
		}
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
