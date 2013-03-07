package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import au.com.mineauz.PlayerSpy.InventoryViewer;
import au.com.mineauz.PlayerSpy.Utilities.Util;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class InventoryCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "inventory";
	}

	@Override
	public String[] getAliases()
	{
		return new String[] {"inv"};
	}

	@Override
	public String getPermission()
	{
		return "playerspy.inventory";
	}

	@Override
	public String getUsageString( String label )
	{
		return label + ChatColor.GOLD + " <player> " + ChatColor.GREEN + "[date]";
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

	private void printItem(CommandSender toWho, ItemStack item, String loc)
	{
		if(item == null)
			toWho.sendMessage("    " + loc + ": *empty*");
		else
			toWho.sendMessage("    " + loc + ": " + Utility.formatItemName(item) + "x" + item.getAmount());
	}
	public void showInventory(CommandSender toWho, Inventory inv, OfflinePlayer offlineOwner, boolean canEdit)
	{
		if(toWho instanceof Player)
		{
			InventoryViewer.openInventory(inv, (Player)toWho, offlineOwner, canEdit );
		}
		else
		{
			toWho.sendMessage("Inventory of " + inv.getTitle() + ":");
			toWho.sendMessage("  Hotbar:");
			for(int i = 0; i < 9; ++i)
				printItem(toWho, inv.getItem(i), "" + i);

			toWho.sendMessage("");
			toWho.sendMessage("  Main Inventory:");
			for(int i = 9; i < 36; ++i)
				printItem(toWho, inv.getItem(i), String.format("%d,%d", (i-9)%9, (i-9)/9));
			
			toWho.sendMessage("  Armour:");
			printItem(toWho, inv.getItem(36), "Helmet");
			printItem(toWho, inv.getItem(37), "Chestplate");
			printItem(toWho, inv.getItem(38), "Leggings");
			printItem(toWho, inv.getItem(39), "Boots");
		}
	}
	@Override
	public boolean onCommand( CommandSender sender, String label, String[] args )
	{
		if(args.length < 1)
			return false;
		
		String playerName = args[0];
		
		long current = System.currentTimeMillis(); 
		long date = current;
		
		if(args.length == 2)
			date = Util.parseDate(args[1], 0, 0, 0);
		else if(args.length == 3)
			date = Util.parseDate(args[1] + " " + args[2], 0, 0, 0);
		
		if(date == 0)
		{
			sender.sendMessage(ChatColor.RED + "Invalid date/time format");
			return true;
		}
		
		if(date >= current)
		{
			Player toView = Bukkit.getPlayer(playerName);
			if(toView != null)
			{
				showInventory(sender, toView.getInventory(), null, sender.hasPermission("playerspy.inventory.edit"));
			}
			else
			{
				OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(playerName);
				
				if(oPlayer.hasPlayedBefore())
				{
					PlayerInventory inv = Utility.getOfflinePlayerInventory(oPlayer);
					if(inv != null)
						showInventory(sender, inv, oPlayer, sender.hasPermission("playerspy.inventory.edit"));
					else
					{
						sender.sendMessage(ChatColor.RED + playerName + "'s inventory is unavailable.");
						return true;
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "Unknown player: " + playerName);
					return true;
				}
			}
		}
		else
		{
			sender.sendMessage("Not yet implemented");
			return true;
			// TODO: implement this
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
