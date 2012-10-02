package au.com.mineauz.PlayerSpy.monitoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import au.com.mineauz.PlayerSpy.InventorySlot;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.Pair;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Utility;
import au.com.mineauz.PlayerSpy.Records.UpdateInventoryRecord;

/**
 * Keeps track of the flow of items across the server.
 */
public class ItemFlowTracker implements Listener
{
	@EventHandler
	private void onItemDespawn(ItemDespawnEvent event)
	{
		
	}
	
	@EventHandler
	private void onItemSpawn(ItemSpawnEvent event)
	{
		
	}
	
	@EventHandler
	private void onInventoryClick(InventoryClickEvent event)
	{
		if(!(event.getWhoClicked() instanceof Player))
			return;
		
		
		int lastClick = (mPlayerLastClick.containsKey((Player)event.getWhoClicked()) ? mPlayerLastClick.get((Player)event.getWhoClicked()) : -999);
		int currentClick = event.getRawSlot();

		Inventory source = (lastClick == -999 ? null : (lastClick < event.getView().getTopInventory().getSize() ? event.getView().getTopInventory() : event.getView().getBottomInventory()));
		Inventory dest = (currentClick == -999 ? null : (currentClick < event.getView().getTopInventory().getSize() ? event.getView().getTopInventory() : event.getView().getBottomInventory()));

		if(mCurrentTransactions.get((Player)event.getWhoClicked()) != null)
		{
			Inventory inv = mCurrentTransactions.get((Player)event.getWhoClicked()).getArg2();
			source = inv;
		}
		
		if(source == null && dest == null)
			return;

		
		
		if(event.isShiftClick())
		{
			if(Utility.getStackOrNull(event.getCursor()) != null)
				// Sideline the transaction
				mAltTransactions.put((Player)event.getWhoClicked(), mCurrentTransactions.get((Player)event.getWhoClicked()));

			// Create a new transaction
			mCurrentTransactions.put((Player)event.getWhoClicked(),new Pair<ItemStack, Inventory>((event.getCurrentItem() == null ? null : event.getCurrentItem().clone()), dest));
			if(dest == event.getView().getTopInventory())
				scheduleTransactionInInventory((Player)event.getWhoClicked(), event.getView().getBottomInventory());
			else
				scheduleTransactionInInventory((Player)event.getWhoClicked(), event.getView().getTopInventory());
		}
		else
		{
			// Picking the item up from the inventory
			if(source == null && dest != null) 
			{
				if(Utility.getStackOrNull(event.getCursor()) != null) // Still holding an item for some reason
				{
					LogUtil.fine(event.getCursor().toString());
					scheduleTransactionInSlot((Player)event.getWhoClicked(), dest, event.getSlot());
				}
				else
					scheduleTransactionInCursor((Player)event.getWhoClicked(), dest);
				
				mPlayerLastClick.put((Player)event.getWhoClicked(), currentClick);
			}
			// Throwing the item out
			else if(source != null && dest == null)
			{
				scheduleTransactionInCursor((Player)event.getWhoClicked(), null);
			}
			// Transfering the item between inventories
			else if(source != dest)
			{
				scheduleTransactionInSlot((Player)event.getWhoClicked(), dest, event.getSlot());
			}
			else
			{
				mPlayerLastClick.put((Player)event.getWhoClicked(), -999);
			}
		}
		
		scheduleInventoryUpdate(event.getView().getBottomInventory());
	}
	@EventHandler
	private void onInventoryOpen(InventoryOpenEvent event)
	{
		if(!(event.getPlayer() instanceof Player))
			return;
	
		if(event.getInventory().getType() == InventoryType.CRAFTING || event.getInventory().getType() == InventoryType.CREATIVE)
			return;
		if(event.getInventory().getType() == InventoryType.PLAYER && event.getInventory().getHolder() == event.getPlayer())
			return;
		ShallowMonitor mon = GlobalMonitor.instance.getMonitor((Player)event.getPlayer());
		if(mon != null)
			mon.beginTransaction(event.getInventory());
	}
	@EventHandler
	private void onInventoryClose(InventoryCloseEvent event)
	{
		if(!(event.getPlayer() instanceof Player))
			return;
	
		if(event.getInventory().getType() == InventoryType.CRAFTING || event.getInventory().getType() == InventoryType.CREATIVE)
			return;
		
		if(event.getInventory().getType() == InventoryType.PLAYER && event.getInventory().getHolder() == event.getPlayer())
			return;
		ShallowMonitor mon = GlobalMonitor.instance.getMonitor((Player)event.getPlayer());
		if(mon != null)
			mon.endTransaction();
		
		scheduleTransactionInCursor((Player)event.getPlayer(), null);
		mPlayerLastClick.put((Player)event.getPlayer(), -999);
	}
	
	public void recordInventoryState(Inventory inventory)
	{
		if(!mLastRecordedState.containsKey(inventory))
		{
			ItemStack[] items = inventory.getContents().clone();
			// Deep copy
			for(int i = 0; i < items.length; i++)
			{
				if(items[i] != null)
					items[i] = items[i].clone();
			}
			
			mLastRecordedState.put(inventory, items);
		}
		else
			applyInventoryChanges(inventory, detectChanges(inventory, mLastRecordedState.get(inventory), false));
	}
	private void scheduleTransactionInCursor(Player who, Inventory inventory)
	{
		if(!mTransactionUpdates.containsKey(who))
		{
			mTransactionUpdates.put(who, Bukkit.getScheduler().scheduleSyncDelayedTask(SpyPlugin.getInstance(),new TransactionUpdate(who, inventory)));
		}
	}
	private void scheduleTransactionInSlot(Player who, Inventory inventory, int slot)
	{
		if(!mTransactionUpdates.containsKey(who))
		{
			mTransactionUpdates.put(who, Bukkit.getScheduler().scheduleSyncDelayedTask(SpyPlugin.getInstance(),new TransactionUpdate(who, inventory, slot)));
		}
	}
	private void scheduleTransactionInInventory(Player who, Inventory inventory)
	{
		if(!mTransactionUpdates.containsKey(who))
		{
			recordInventoryState(inventory);
			mTransactionUpdates.put(who, Bukkit.getScheduler().scheduleSyncDelayedTask(SpyPlugin.getInstance(),new TransactionUpdate(who, inventory, -2)));
		}
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
			return null;
		
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
				islot.Item = (newStack != null ? newStack.clone() : newStack);
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
		}
		
		if(handled)
		{
			
		}
	}
	public void recordInventoryChanges(Inventory inventory, InventorySlot... changes )
	{
		recordInventoryChanges(inventory, new ArrayList<InventorySlot>(Arrays.asList(changes)));
	}

	private HashMap<Player, Integer> mTransactionUpdates = new HashMap<Player, Integer>();
	private HashMap<Player, Pair<ItemStack, Inventory>> mCurrentTransactions = new HashMap<Player, Pair<ItemStack, Inventory>>();
	private HashMap<Player, Pair<ItemStack, Inventory>> mAltTransactions = new HashMap<Player, Pair<ItemStack, Inventory>>();
	private HashMap<Player, Integer> mPlayerLastClick = new HashMap<Player, Integer>();
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
	
	/**
	 * Handles Processing transactions with inventories
	 */
	private class TransactionUpdate implements Runnable
	{
		private final Inventory mInventory;
		private final int mSlot;
		private final Player mWho;
		
		private ItemStack mLastCursor;
		private ItemStack mLastSlot;
		public TransactionUpdate(Player who, Inventory inventory, int slot)
		{
			mInventory = inventory;
			mSlot = slot;
			mWho = who;
			
			mLastCursor = who.getItemOnCursor().clone();
			if(mLastCursor.getTypeId() == 0)
				mLastCursor = null;
			
			if(mInventory != null && mSlot >= 0)
				mLastSlot = (inventory.getItem(slot) == null ? null : inventory.getItem(slot).clone());
			else
				mLastSlot = null;
		}
		public TransactionUpdate(Player who, Inventory inventory)
		{
			this(who, inventory, -1);
		}
		
		@Override
		public void run() 
		{
			ShallowMonitor mon = GlobalMonitor.instance.getMonitor(mWho);
			
			if(mInventory != null && mSlot >= 0) // Slot updates needed
			{
				ItemStack current = (mInventory.getItem(mSlot) == null ? null : mInventory.getItem(mSlot).clone());
				
				if(current != null && !current.equals(mLastSlot)) // The slot has changed
				{
					if(Utility.areEqualIgnoreAmount(mLastCursor, mWho.getItemOnCursor()) && mLastCursor != null)
					{
						// Only some were put into the slot
						int difference = mLastCursor.getAmount() - mWho.getItemOnCursor().getAmount();
						if(mon != null)
							mon.doTransaction(new ItemStack(mLastCursor.getType(), difference, mLastCursor.getDurability()), mInventory == mWho.getInventory());

						// Update the current transaction
						mCurrentTransactions.get(mWho).getArg1().setAmount(mWho.getItemOnCursor().getAmount());
					}
					else if(Utility.getStackOrNull(mWho.getItemOnCursor()) == null)
					{
						// Put all into the inventory
						if(mon != null)
							mon.doTransaction(mLastCursor, mInventory == mWho.getInventory());
						
						// Update the current transaction
						mCurrentTransactions.put(mWho, null);
						mPlayerLastClick.put(mWho, -999);
					}
					else
					{
						// Put all into the inventory, and picked up what was there
						if(mon != null)
							mon.doTransaction(mLastCursor, mInventory == mWho.getInventory());
						
						// Update the current transaction
						mCurrentTransactions.put(mWho, new Pair<ItemStack, Inventory>(mWho.getItemOnCursor().clone(), mInventory));
					}
				}
			}
			else if(mInventory != null && mSlot == -2) // Inventory updates needed
			{
				ArrayList<InventorySlot> changes = detectChanges(mInventory, true);
				int totalToTake = 0;
				for(InventorySlot change : changes)
				{
					if(change.Item == null)
						continue;
					
					totalToTake += change.Item.getAmount();
				}
				
				if(totalToTake != 0)
				{
					ItemStack transStack = mCurrentTransactions.get(mWho).getArg1();
					transStack.setAmount(totalToTake);
					// Log the transaction
					if(mon != null)
						mon.doTransaction(transStack, mInventory == mWho.getInventory());
				}
				
				// Restore the last transaction
				mCurrentTransactions.put(mWho, mAltTransactions.remove(mWho));
			}
			else
			{
				if(Utility.getStackOrNull(mWho.getItemOnCursor()) == null && mLastCursor != null)
				{
					LogUtil.finer("Thrown out");
					// Item thrown out
					if(mon != null)
						mon.doTransaction(mLastCursor, mInventory == mWho.getInventory());
					
					// Update the current transaction
					mCurrentTransactions.put(mWho, null);
					mPlayerLastClick.put(mWho, -999);
				}
				else if(mLastCursor == null && Utility.getStackOrNull(mWho.getItemOnCursor()) != null)
				{
					// Item picked up
					mCurrentTransactions.put(mWho, new Pair<ItemStack, Inventory>(mWho.getItemOnCursor().clone(), mInventory));
					LogUtil.finer("Picked up " + mWho.getItemOnCursor().toString());
				}
			}
			
			mTransactionUpdates.remove(mWho);
		}
	}
}