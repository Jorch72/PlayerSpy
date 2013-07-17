package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import au.com.mineauz.PlayerSpy.InventoryViewer;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class EnderChestCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "enderchest";
	}

	@Override
	public String[] getAliases()
	{
		return new String[] {"chest"};
	}

	@Override
	public String getPermission()
	{
		return "playerspy.enderchest";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender)
	{
		return new String[] {label + ChatColor.GOLD + " <player> "};
	}
	
	@Override
	public String getDescription()
	{
		return "Displays the enderchest for a player.";
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

	
	public void showInventory(CommandSender toWho, Inventory inv, OfflinePlayer offlineOwner, boolean canEdit)
	{
		if(toWho instanceof Player)
			InventoryViewer.openInventory(inv, (Player)toWho, offlineOwner, canEdit );
		else
			InventoryViewer.printInventory(inv, toWho);
	}
	@Override
	public boolean onCommand( CommandSender sender, String label, String[] args )
	{
		if(args.length != 1)
			return false;
		
		String playerName = args[0];

		Player toView = Bukkit.getPlayer(playerName);
		if(toView != null)
		{
			showInventory(sender, toView.getEnderChest(), null, sender.hasPermission("playerspy.enderchest.edit"));
		}
		else
		{
			OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(playerName);
			
			if(oPlayer.hasPlayedBefore())
			{
				Inventory inv = Utility.getOfflinePlayerEnderchest(oPlayer);
				if(inv != null)
					showInventory(sender, inv, oPlayer, sender.hasPermission("playerspy.enderchest.edit"));
				else
				{
					sender.sendMessage(ChatColor.RED + playerName + "'s enderchest is unavailable.");
					return true;
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + "Unknown player: " + playerName);
				return true;
			}
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
