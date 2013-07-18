package au.com.mineauz.PlayerSpy.monitoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.InventoryRecord;
import au.com.mineauz.PlayerSpy.Records.UpdateInventoryRecord;
import au.com.mineauz.PlayerSpy.monitoring.trackers.ItemTracker;
import au.com.mineauz.PlayerSpy.storage.InventorySlot;


/**
 * Keeps track of the flow of items across the server.
 */
public class ItemFlowTracker implements Listener
{
	public void updateInventoryStates()
	{
		for(Player player : Bukkit.getOnlinePlayers())
		{
			ShallowMonitor monitor = GlobalMonitor.instance.getMonitor(player);
			
			if(monitor == null)
				continue;
			
			if(!mLastRecordedState.containsKey(player.getInventory()))
			{
				monitor.logRecord(new InventoryRecord(player.getInventory()));
				recordInventoryState(player.getInventory());
			}
			else
			{
				ArrayList<InventorySlot> slots = detectChanges(player.getInventory(), false);
				
				if(!slots.isEmpty())
					monitor.logRecord(new UpdateInventoryRecord(slots));
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onInventoryClick(InventoryClickEvent event)
	{
		if(!(event.getWhoClicked() instanceof Player))
			return;
		
		ShallowMonitor monitor = GlobalMonitor.instance.getMonitor((Player)event.getWhoClicked());
		
		// Only deep monitors do per click updates
		if(monitor instanceof DeepMonitor)
			scheduleInventoryUpdate(event.getView().getBottomInventory());
		
		// There is no event for opening your own inventory, so this should be a good replacement
		else if(!mLastRecordedState.containsKey(event.getView().getBottomInventory()))
			recordInventoryState(event.getView().getBottomInventory());
		
		if(event.getInventory().getType() == InventoryType.CRAFTING || event.getInventory().getType() == InventoryType.CREATIVE)
			return;
		
		if(event.getView().getType() == InventoryType.PLAYER && event.getInventory().getHolder() == event.getWhoClicked())
			return;
		
		ItemTracker tracker = monitor.getItemTracker();
		
		if(!tracker.isInventoryOpen()) // Inventory open event didnt fire
			tracker.onOpenInventory(event.getView(), null);
		
		switch(event.getAction())
		{
		// These ones affect the entire view
		case COLLECT_TO_CURSOR:
			tracker.collect();
			break;
		case MOVE_TO_OTHER_INVENTORY:
			tracker.transfer(event.getRawSlot());
			break;
			
		// These ones affect only the cursor
		case DROP_ALL_CURSOR:
			tracker.dropCursor(event.getCursor().getAmount());
			break;
		case DROP_ONE_CURSOR:
			tracker.dropCursor(1);
			break;
		// These ones affect a slot
		case DROP_ALL_SLOT:
			tracker.dropSlot(event.getRawSlot(), event.getCurrentItem().getAmount());
			break;
		case DROP_ONE_SLOT:
			tracker.dropSlot(event.getRawSlot(), 1);
			break;

		// These ones affect both slot and cursor
		case PICKUP_ALL:
			tracker.pickup(event.getRawSlot(), event.getCurrentItem().getAmount());
			break;
		case PICKUP_HALF:
			tracker.pickup(event.getRawSlot(), (int)(event.getCurrentItem().getAmount() / 2D + 0.5));
			break;
		case PICKUP_ONE:
			tracker.pickup(event.getRawSlot(), 1);
			break;
		case PICKUP_SOME:
			tracker.pickup(event.getRawSlot(), Math.min((event.getCursor() != null ? event.getCursor().getMaxStackSize() - event.getCursor().getAmount() : 64), Math.min(event.getCurrentItem().getAmount(), event.getCurrentItem().getMaxStackSize())));
			break;
		case PLACE_ALL:
			tracker.place(event.getRawSlot(), event.getCursor().getAmount());
			break;
		case PLACE_ONE:
			tracker.place(event.getRawSlot(), 1);
			break;
		case PLACE_SOME:
			tracker.place(event.getRawSlot(), Math.min((event.getCurrentItem() != null ? event.getCurrentItem().getMaxStackSize() - event.getCurrentItem().getAmount() : 64), Math.min(event.getCursor().getAmount(), event.getCursor().getMaxStackSize())));
			break;
		case SWAP_WITH_CURSOR:
			tracker.swap(event.getRawSlot());
			break;
			
		// These ones are special
		case CLONE_STACK:
			break;
		case HOTBAR_MOVE_AND_READD:
			tracker.pickup(event.getRawSlot(), event.getCurrentItem().getAmount());
			tracker.transfer(27 + event.getHotbarButton() + event.getView().getTopInventory().getSize());
			tracker.place(27 + event.getHotbarButton() + event.getView().getTopInventory().getSize(), event.getCurrentItem().getAmount());
			break;
		case HOTBAR_SWAP:
			tracker.swap(event.getRawSlot(), event.getHotbarButton());
			break;
		
		default:
			return;
		}
		
	}
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onInventoryDrag(InventoryDragEvent event)
	{
		if(!(event.getWhoClicked() instanceof Player))
			return;
		
		ShallowMonitor monitor = GlobalMonitor.instance.getMonitor((Player)event.getWhoClicked());
		
		// Only deep monitors do per click updates
		if(monitor instanceof DeepMonitor)
			scheduleInventoryUpdate(event.getView().getBottomInventory());
		
		// There is no event for opening your own inventory, so this should be a good replacement
		else if(!mLastRecordedState.containsKey(event.getView().getBottomInventory()))
			recordInventoryState(event.getView().getBottomInventory());
		
		if(event.getInventory().getType() == InventoryType.CREATIVE)
			return;
		
		if(event.getView().getType() == InventoryType.PLAYER && event.getInventory().getHolder() == event.getWhoClicked())
			return;
		
		ItemTracker tracker = monitor.getItemTracker();
		
		if(!tracker.isInventoryOpen()) // Inventory open event didnt fire
			tracker.onOpenInventory(event.getView(), null);
		
		Map<Integer, ItemStack> slots = event.getNewItems();
		
		ArrayList<InventorySlot> dragResult = new ArrayList<InventorySlot>();
		for(Entry<Integer, ItemStack> changed : slots.entrySet())
		{
			ItemStack original = event.getView().getItem(changed.getKey());
			ItemStack newStack = changed.getValue().clone();
			
			if(original.isSimilar(newStack))
				newStack.setAmount(newStack.getAmount() - original.getAmount());
			
			dragResult.add(new InventorySlot(newStack, changed.getKey()));
		}
		
		tracker.drag(dragResult);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onInventoryOpen(InventoryOpenEvent event)
	{
		if(!(event.getPlayer() instanceof Player))
			return;

		recordInventoryState(event.getView().getBottomInventory());
		
		if(event.getInventory().getType() == InventoryType.CREATIVE)
			return;
		if(event.getInventory().getType() == InventoryType.PLAYER && event.getInventory().getHolder() == event.getPlayer())
			return;
		
		// Record the state of their inventory
		if(event.getInventory().getType() == InventoryType.PLAYER)
			recordInventoryState(event.getView().getTopInventory());
		
		ShallowMonitor mon = GlobalMonitor.instance.getMonitor((Player)event.getPlayer());
		if(mon == null)
			return;
		
		Location enderChestLocation = null;

		Block block = event.getPlayer().getTargetBlock(null, 100);
		if(block != null && block.getLocation().distance(event.getPlayer().getLocation()) >= 7)
			block = null;
		
		if(block != null)
		{
			switch(event.getInventory().getType())
			{
			case ENDER_CHEST:
				if(block.getType() == Material.ENDER_CHEST)
					enderChestLocation = block.getLocation();
				break;
			case WORKBENCH:
			case CRAFTING:
				if(block.getType() == Material.WORKBENCH)
					enderChestLocation = block.getLocation();
				break;
			case ENCHANTING:
				if(block.getType() == Material.ENCHANTMENT_TABLE)
					enderChestLocation = block.getLocation();
				break;
			case ANVIL:
				if(block.getType() == Material.ANVIL)
					enderChestLocation = block.getLocation();
				break;
			default:
				break;
			}
		}
		
		ItemTracker tracker = mon.getItemTracker();
		tracker.onOpenInventory(event.getView(), enderChestLocation);
	}
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onInventoryClose(InventoryCloseEvent event)
	{
		if(!(event.getPlayer() instanceof Player))
			return;
	
		if(event.getInventory().getType() == InventoryType.CRAFTING || event.getInventory().getType() == InventoryType.CREATIVE)
		{
			ArrayList<InventorySlot> slots = detectChanges(event.getView().getBottomInventory(), false);
			recordInventoryChanges(event.getView().getBottomInventory(), slots);
			applyInventoryChanges(event.getView().getBottomInventory(), slots);
			return;
		}
		
		if(event.getInventory().getType() == InventoryType.PLAYER && event.getInventory().getHolder() == event.getPlayer())
		{
			ArrayList<InventorySlot> slots = detectChanges(event.getView().getBottomInventory(), false);
			recordInventoryChanges(event.getView().getBottomInventory(), slots);
			applyInventoryChanges(event.getView().getBottomInventory(), slots);
			return;
		}
		if(event.getInventory().getType() == InventoryType.PLAYER)
		{
			// When interacting with another players inventory, update theirs too
			scheduleInventoryUpdate(event.getView().getTopInventory());
		}
		
		ShallowMonitor mon = GlobalMonitor.instance.getMonitor((Player)event.getPlayer());
		if(mon == null)
			return;
		
		ItemTracker tracker = mon.getItemTracker();
		tracker.onCloseInventory();
	}
	
	public void recordInventoryState(Inventory inventory)
	{
		if(!mLastRecordedState.containsKey(inventory))
		{
			int count = inventory.getContents().length;
			if(inventory instanceof PlayerInventory)
				count += ((PlayerInventory)inventory).getArmorContents().length;

			ItemStack[] items = new ItemStack[count];
			
			// Deep copy
			for(int i = 0; i < items.length; i++)
			{
				if(i < inventory.getContents().length)
				{
					if(inventory.getContents()[i] != null)
						items[i] = inventory.getContents()[i].clone();
				}
				else
				{
					if(((PlayerInventory)inventory).getArmorContents()[i-inventory.getContents().length] != null)
						items[i] = ((PlayerInventory)inventory).getArmorContents()[i-inventory.getContents().length].clone();
				}
			}
			
			mLastRecordedState.put(inventory, items);
		}
		else
			applyInventoryChanges(inventory, detectChanges(inventory, mLastRecordedState.get(inventory), false));
	}
	
	/**
	 * Schedule an update to check the changes and record them for an inventory
	 * @param inventory The inventory to check
	 */
	public void scheduleInventoryUpdate(Inventory inventory)
	{
		if(!mScheduledUpdates.containsKey(inventory))
		{
			recordInventoryState(inventory);
			mScheduledUpdates.put(inventory, Bukkit.getScheduler().scheduleSyncDelayedTask(SpyPlugin.getInstance(), new InventoryUpdate(inventory)));
		}
	}
	
	/**
	 * Finds all the changes made to an inventory since the last time it was recorded or checked
	 * @return An arraylist of all changes detected or null if the state has never been recorded
	 */
	public ArrayList<InventorySlot> detectChanges(Inventory inventory, boolean diff)
	{
		// Get the last recorded state
		ItemStack[] stored = mLastRecordedState.get(inventory);
		if(stored == null)
			return new ArrayList<InventorySlot>();
		
		// Detect the changes
		ArrayList<InventorySlot> changes = detectChanges(inventory, stored, diff);
		// Update the last recorded state
		recordInventoryState(inventory);
		
		return changes;
	}
	/**
	 * Finds all the changes made to an inventory since the stored state
	 * @param current The inventory as it is now
	 * @param stored The contents of the inventory at some point in time ago
	 * @return An arraylist of all changes detected
	 */
	private ArrayList<InventorySlot> detectChanges(Inventory current, ItemStack[] stored, boolean diff)
	{
		ArrayList<InventorySlot> changes = new ArrayList<InventorySlot>();
		
		if(current instanceof PlayerInventory)
		{
			int offset = ((PlayerInventory)current).getContents().length;
			
			for(int slot = 0; slot < ((PlayerInventory)current).getArmorContents().length; slot++)
			{
				ItemStack oldStack = stored[slot + offset];
				ItemStack newStack = ((PlayerInventory)current).getArmorContents()[slot];
				
				// Detect changes
				boolean changed = false;
				
				if((oldStack == null && newStack != null) ||
				   (oldStack != null && newStack == null))
					changed = true;
				else if(oldStack != null && newStack != null)
				{
					if(!oldStack.equals(newStack))
						changed = true;
				}
				
				if(!changed)
					continue;
				
				InventorySlot islot = new InventorySlot();
				islot.Slot = slot + offset;
				if(diff)
				{
					if(oldStack == null)
						islot.Item = newStack.clone();
					else if(newStack == null)
					{
						islot.Item = oldStack.clone();
						islot.Item.setAmount(-islot.Item.getAmount());
					}
					else
					{
						islot.Item = new ItemStack(oldStack.getType(), newStack.getAmount() - oldStack.getAmount(), oldStack.getDurability());
					}
				}
				else
					islot.Item = (newStack != null ? newStack.clone() : null);
				changes.add(islot);
			}
		}
		
		for(int slot = 0; slot < current.getContents().length; slot++)
		{
			ItemStack oldStack = stored[slot];
			ItemStack newStack = current.getContents()[slot];
			
			// Detect changes
			boolean changed = false;
			
			if((oldStack == null && newStack != null) ||
			   (oldStack != null && newStack == null))
				changed = true;
			else if(oldStack != null && newStack != null)
			{
				if(!oldStack.equals(newStack))
					changed = true;
			}
			
			if(!changed)
				continue;
			
			InventorySlot islot = new InventorySlot();
			islot.Slot = slot;
			if(diff)
			{
				if(oldStack == null)
					islot.Item = newStack.clone();
				else if(newStack == null)
				{
					islot.Item = oldStack.clone();
					islot.Item.setAmount(-islot.Item.getAmount());
				}
				else
				{
					islot.Item = new ItemStack(oldStack.getType(), newStack.getAmount() - oldStack.getAmount(), oldStack.getDurability());
				}
			}
			else
				islot.Item = (newStack != null ? newStack.clone() : null);
			changes.add(islot);
		}
		
		return changes;
	}
	/**
	 * Applies the changes to the last recorded state for that inventory
	 */
	private void applyInventoryChanges(Inventory inventory, ArrayList<InventorySlot> changes)
	{
		if(!mLastRecordedState.containsKey(inventory))
			return;
		
		ItemStack[] slots = mLastRecordedState.get(inventory);
		
		for(InventorySlot change : changes)
			slots[change.Slot] = change.Item;
	}
	public void recordInventoryChanges(Inventory inventory, ArrayList<InventorySlot> changes)
	{
		if(changes == null || changes.isEmpty())
			return;
		
		boolean handled = false;
		if(inventory instanceof PlayerInventory)
		{
			HumanEntity holder = ((PlayerInventory)inventory).getHolder();
			if(holder instanceof Player)
			{
				ShallowMonitor mon = GlobalMonitor.instance.getMonitor((Player)holder);
				if(mon != null)
				{
					mon.logRecord(new UpdateInventoryRecord(changes));
					handled = true;
				}
			}
			else
				GlobalMonitor.instance.logRecord(new UpdateInventoryRecord(changes), Cause.playerCause(Bukkit.getOfflinePlayer(holder.getName())), null);
		}
		
		if(handled)
		{
			
		}
	}
	public void recordInventoryChanges(Inventory inventory, InventorySlot... changes )
	{
		recordInventoryChanges(inventory, new ArrayList<InventorySlot>(Arrays.asList(changes)));
	}

	private HashMap<Inventory, Integer> mScheduledUpdates = new HashMap<Inventory, Integer>();
	private HashMap<Inventory, ItemStack[]> mLastRecordedState = new HashMap<Inventory, ItemStack[]>();
	
	/**
	 * Handles logging exact changes to inventories
	 */
	private class InventoryUpdate implements Runnable
	{
		private final Inventory mInventory;
		public InventoryUpdate(Inventory inventory)
		{
			mInventory = inventory;
		}
		@Override
		public void run() 
		{
			ArrayList<InventorySlot> changes = detectChanges(mInventory, false);
			
			UpdateInventoryRecord record = new UpdateInventoryRecord(changes);
			if(mInventory instanceof PlayerInventory)
			{
				HumanEntity ent = ((PlayerInventory)mInventory).getHolder();
				if(ent instanceof Player)
				{
					ShallowMonitor mon = GlobalMonitor.instance.getMonitor((Player)ent);
					if(mon != null)
						mon.logRecord(record);
				}
				
			}
			mScheduledUpdates.remove(mInventory);
		}
	}
}
