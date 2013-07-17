package au.com.mineauz.PlayerSpy;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;
import au.com.mineauz.PlayerSpy.storage.InventorySlot;

public class InventoryViewer
{
	private static EventHandler mHandler;
	private static HashMap<Player, InventoryState> mOpenInventories = new HashMap<Player, InventoryState>();
	
	public static void initialize()
	{
		mHandler = new EventHandler();
		mHandler.initialize();
	}
	
	public static void openInventory(Inventory inventory, Player viewer, OfflinePlayer offlineOwner, boolean canEdit)
	{
		InventoryState state = new InventoryState();
		state.canEdit = canEdit;
		state.inventory = inventory;
		state.offlineOwner = offlineOwner;
		
		mOpenInventories.put(viewer, state);
		
		if(offlineOwner != null)
			GlobalMonitor.instance.getItemFlowTracker().recordInventoryState(inventory);
		viewer.openInventory(inventory);
	}
	
	private static void printItem(CommandSender toWho, ItemStack item, String loc)
	{
		if(item == null)
			toWho.sendMessage("    " + loc + ": *empty*");
		else
			toWho.sendMessage("    " + loc + ": " + Utility.formatItemName(item) + "x" + item.getAmount());
	}
	
	public static void printInventory(Inventory inventory, CommandSender viewer)
	{
		viewer.sendMessage(inventory.getTitle());
		viewer.sendMessage("  Hotbar:");
		for(int i = 0; i < 9; ++i)
			printItem(viewer, inventory.getItem(i), "" + i);

		viewer.sendMessage("");
		viewer.sendMessage("  Main Inventory:");
		for(int i = 9; i < 36; ++i)
			printItem(viewer, inventory.getItem(i), String.format("%d,%d", (i-9)%9, (i-9)/9));
		
		viewer.sendMessage("  Armour:");
		printItem(viewer, inventory.getItem(39), "Helmet");
		printItem(viewer, inventory.getItem(38), "Chestplate");
		printItem(viewer, inventory.getItem(37), "Leggings");
		printItem(viewer, inventory.getItem(36), "Boots");
	}
	
	private static class EventHandler implements Listener
	{
		public void initialize()
		{
			Bukkit.getPluginManager().registerEvents(this, SpyPlugin.getInstance());
		}
		
		@org.bukkit.event.EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onInventoryClick(InventoryClickEvent event)
		{
			if(mOpenInventories.containsKey(event.getWhoClicked()))
			{
				InventoryState state = mOpenInventories.get(event.getWhoClicked());
				
				if(event.getRawSlot() < event.getView().getTopInventory().getSize())
				{
					if(!state.canEdit)
					{
						if(event.getClick() == ClickType.MIDDLE)
						{
							event.getWhoClicked().setItemOnCursor(event.getCurrentItem().clone());
						}
						else
						{
							event.setCancelled(true);
							event.setResult(Result.DENY);
						}
						return;
					}
				}
				else 
				{
					switch(event.getAction())
					{
					case COLLECT_TO_CURSOR:
					case MOVE_TO_OTHER_INVENTORY:
					case NOTHING:
					case UNKNOWN:
						event.setCancelled(true);
						event.setResult(Result.DENY);
						break;
					default:
						break;
					}
				}
			}
		}
		@org.bukkit.event.EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onInventoryDrag(InventoryDragEvent event)
		{
			if(mOpenInventories.containsKey(event.getWhoClicked()))
			{
				InventoryState state = mOpenInventories.get(event.getWhoClicked());
			
				boolean crosses = false;
				for(int slot : event.getRawSlots())
				{
					if(slot < event.getView().getTopInventory().getSize())
						crosses = true;
				}
				
				if(!state.canEdit && crosses)
				{
					event.setCancelled(true);
					event.setResult(Result.DENY);
				}
			}
		}
		
		@org.bukkit.event.EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onInventoryClose(InventoryCloseEvent event)
		{
			InventoryState state = mOpenInventories.get(event.getPlayer());
			if(state != null)
			{
				if(state.offlineOwner != null)
				{
					Player onlinePlayer = state.offlineOwner.getPlayer();
					if(onlinePlayer != null)
						onlinePlayer.getInventory().setContents(state.inventory.getContents());
					else
						Utility.setOfflinePlayerInventory(state.offlineOwner, (PlayerInventory)state.inventory);
					
					ArrayList<InventorySlot> changes = GlobalMonitor.instance.getItemFlowTracker().detectChanges(state.inventory, false);
					GlobalMonitor.instance.getItemFlowTracker().recordInventoryChanges(state.inventory, changes);
				}
				mOpenInventories.remove(event.getPlayer());
			}
			
		}
		
		@org.bukkit.event.EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onPlayerQuit(PlayerQuitEvent event)
		{
			InventoryState state = mOpenInventories.get(event.getPlayer());
			if(state != null)
			{
				if(state.offlineOwner != null)
				{
					Player onlinePlayer = state.offlineOwner.getPlayer();
					if(onlinePlayer != null)
						onlinePlayer.getInventory().setContents(state.inventory.getContents());
					else
						Utility.setOfflinePlayerInventory(state.offlineOwner, (PlayerInventory)state.inventory);
				}
				mOpenInventories.remove(event.getPlayer());
			}
		}
		
	}
	
	private static class InventoryState
	{
		public Inventory inventory;
		public boolean canEdit;
		public OfflinePlayer offlineOwner;
	}
}
