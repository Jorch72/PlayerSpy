package au.com.mineauz.PlayerSpy.monitoring.trackers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.InventoryTransactionRecord;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.monitoring.ShallowMonitor;
import au.com.mineauz.PlayerSpy.storage.InventorySlot;
import au.com.mineauz.PlayerSpy.wrappers.craftbukkit.CraftInventoryView;
import au.com.mineauz.PlayerSpy.wrappers.craftbukkit.CraftItemStack;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.Slot;

public class ItemTracker implements Tracker, Listener
{
	private ShallowMonitor mMonitor;
	
	private InventoryView mCurrentView;
	
	private Inventory mCurrentInventory;
	private Location mInventoryLocation; // Player dependent inventories need this
	
	private ArrayList<ItemStack> mTransactions;
	
	private ItemStack mPickedUp;
	private Inventory mPickedUpInventory;
	
	private ArrayList<InventorySlot> mInitialSlots = new ArrayList<InventorySlot>();
	
	public ItemTracker(ShallowMonitor monitor)
	{
		mMonitor = monitor;
	}
	
	private void beginTransaction(Inventory inventory, Location enderChestLocation)
	{
		if(mCurrentInventory != null)
			endTransaction();
		
		mCurrentInventory = inventory;
		mTransactions = new ArrayList<ItemStack>();
		mInventoryLocation = enderChestLocation;
	}
	
	private void doTransaction(ItemStack item, boolean take)
	{
		Debug.loggedAssert(mCurrentInventory != null);
		
		// Total up transactions
		for(ItemStack transaction : mTransactions)
		{
			if(Utility.areEqualIgnoreAmount(transaction, item))
			{
				// Total it up
				if(take)
					transaction.setAmount(transaction.getAmount() - item.getAmount());
				else
					transaction.setAmount(transaction.getAmount() + item.getAmount());
				
				return;
			}
		}
		
		// No match
		ItemStack transaction = item.clone();
		if(take)
			transaction.setAmount(-transaction.getAmount());
		mTransactions.add(transaction);
	}
	private void endTransaction()
	{
		Debug.loggedAssert(mCurrentInventory != null);
		
		// Log what ever is left
		for(ItemStack transaction : mTransactions)
		{
			if(transaction.getAmount() != 0)
			{
				InventoryTransactionRecord record = null;
				if(transaction.getAmount() < 0)
				{
					transaction.setAmount(-transaction.getAmount());
					record = InventoryTransactionRecord.newTakeFromInventory(transaction, mCurrentInventory, mInventoryLocation);
				}
				else
					record = InventoryTransactionRecord.newAddToInventory(transaction, mCurrentInventory, mInventoryLocation);
				
				mMonitor.logRecord(record);
			}
		}
		
		mCurrentInventory = null;
		mInventoryLocation = null;
		mTransactions.clear();
		mTransactions = null;
	}

	public Inventory getCurrentInventory() { return mCurrentInventory; }
	
	public void onOpenInventory(InventoryView inventory, Location location)
	{
		if(mCurrentView != null)
			onCloseInventory();
		
		beginTransaction(inventory.getTopInventory(), location);
		mCurrentView = inventory;
	}
	
	public boolean isInventoryOpen()
	{
		return mCurrentView != null;
	}
	
	public void onCloseInventory()
	{
		endTransaction();
		mCurrentView = null;
	}
	
	/**
	 * Gets the corresponding inventory for a slot
	 */
	private Inventory getInventory(int rawSlot)
	{
		if(rawSlot < mCurrentView.getTopInventory().getSize())
			return mCurrentView.getTopInventory();
		else
			return mCurrentView.getBottomInventory();
	}
	
	private boolean isPlayersInventory(Inventory inventory)
	{
		return inventory.equals(mCurrentView.getBottomInventory());
	}

	private boolean slotAcceptsItem(int rawSlot, ItemStack item)
	{
		int slot = mCurrentView.convertSlot(rawSlot);
		
		CraftInventoryView view = CraftInventoryView.castFrom(mCurrentView);
		Slot slotObj = view.getHandle().getSlot(slot);
		
		au.com.mineauz.PlayerSpy.wrappers.minecraft.ItemStack itemNMS = CraftItemStack.asNMSCopy(item);
		
		return slotObj.isAllowed(itemNMS);
	}
	
	/**
	 * Collect to cursor (Double click with item in cursor)
	 * Tasks to do: 
	 * Find all stacks in the view that match the type in the cursor
	 * Record their initial values and compare after 1 tick
	 */
	public void collect()
	{
		int toPickup = mPickedUp.getMaxStackSize() - mPickedUp.getAmount();
		
		for(int i = 0; i < mCurrentView.countSlots(); ++i)
		{
			ItemStack item = mCurrentView.getItem(i);
			if(mPickedUp.isSimilar(item))
			{
				item = item.clone();
				int count = Math.min(toPickup, item.getAmount());
				toPickup -= count;
				mPickedUp.setAmount(mPickedUp.getAmount() + count);
				
				item.setAmount(count);
				
				if(!isPlayersInventory(getInventory(i)))
					doTransaction(item, true);
			}
		}
	}
	
	/**
	 * Transfers from this slot to all others (Shift click on slot)
	 * Tasks to do:
	 * Find all empty slots and slots containing the same item
	 * Record their initial values and compare after 1 tick
	 * @param fromSlot The raw slot id that was clicked
	 */
	public void transfer(int fromSlot)
	{
		final ItemStack existing = mCurrentView.getItem(fromSlot).clone();
		final Inventory source = getInventory(fromSlot);
		// Record the initial states so we can compare later
		mInitialSlots.clear();
		
		for(int i = 0; i < mCurrentView.countSlots(); ++i)
		{
			ItemStack item = mCurrentView.getItem(i);
			if(existing.isSimilar(item) || item.getType() == Material.AIR)
				mInitialSlots.add(new InventorySlot(item.clone(), i));
		}
		
		Bukkit.getScheduler().runTask(SpyPlugin.getInstance(), new Runnable()
		{
			@Override
			public void run()
			{
				if(!isInventoryOpen())
					return;
				
				for(InventorySlot slot : mInitialSlots)
				{
					ItemStack item = mCurrentView.getItem(slot.Slot);
					
					if(item.isSimilar(existing) && !slot.Item.equals(item))
					{
						Inventory dest = getInventory(slot.Slot);
						
						ItemStack difference = item.clone();
						if(slot.Item != null)
							difference.setAmount(item.getAmount() - slot.Item.getAmount());
						
						if(dest != source)
							doTransaction(difference, isPlayersInventory(dest));
					}
				}
			}
		});
	}
	
	/**
	 * Drops the contents of the cursor to the ground (click outside window)
	 * Tasks to do:
	 * If the source of the cursor is from the other inv, record the transaction
	 * @param count The amount of items being dropped
	 */
	public void dropCursor(int count)
	{
		if(!isPlayersInventory(mPickedUpInventory))
		{
			ItemStack item = mPickedUp.clone();
			item.setAmount(count);
			
			doTransaction(item, true);
		}
		
		mPickedUp.setAmount(mPickedUp.getAmount() - count);
		if(mPickedUp.getAmount() <= 0)
		{
			mPickedUp = null;
			mPickedUpInventory = null;
		}
	}
	
	/**
	 * Drops the contents of the slot to the ground (alt + click slot)
	 * Tasks to do:
	 * If the slot is in the other inventory, record it
	 * @param fromSlot The raw slot number that was clicked
	 * @param count The amount being dropped
	 */
	public void dropSlot(int fromSlot, int count)
	{
		if(!isPlayersInventory(getInventory(fromSlot)))
		{
			ItemStack item = mCurrentView.getItem(fromSlot).clone();
			item.setAmount(count);
			
			doTransaction(item, true);
		}
	}
	
	/**
	 * Picks up the slot into the cursor. (Left or right click)
	 * @param fromSlot The raw slot number being picked up from
	 * @param count The amount being picked up
	 */
	public void pickup(int fromSlot, int count)
	{
		ItemStack item = mCurrentView.getItem(fromSlot).clone();
		item.setAmount(count);
		
		Inventory source = getInventory(fromSlot);
		
		mPickedUp = item;
		mPickedUpInventory = source;
	}
	
	/**
	 * Puts the contents of the cursor into the slot
	 * @param toSlot The raw slot number being placed into
	 * @param count The number of items being placed
	 */
	public void place(int toSlot, int count)
	{
		if(!slotAcceptsItem(toSlot, mPickedUp))
			return;
		
		Inventory dest = getInventory(toSlot);
		
		ItemStack item = mPickedUp.clone();
		item.setAmount(count);
		
		Inventory source = mPickedUpInventory;
		mPickedUp.setAmount(mPickedUp.getAmount() - count);
		if(mPickedUp.getAmount() <= 0)
		{
			mPickedUp = null;
			mPickedUpInventory = null;
		}
		
		if(dest == source)
			return;

		doTransaction(item, isPlayersInventory(dest));
	}
	
	/**
	 * Swaps the item in the slot with the cursor(left or right click on slot with incompatible item)
	 * @param rawSlot The raw slot number being swapped with
	 */
	public void swap(int rawSlot)
	{
		ItemStack item = mCurrentView.getItem(rawSlot).clone();
		
		Inventory dest = getInventory(rawSlot);
		
		if(dest != mPickedUpInventory)
		{
			doTransaction(item, isPlayersInventory(dest));
			doTransaction(mPickedUp, isPlayersInventory(dest));
			
			mPickedUp = item;
			mPickedUpInventory = dest;
		}
	}
	
	/**
	 * Swaps a stack with a slot on the hotbar
	 * @param rawSlot The raw slot number being swapped with
	 * @param hotBarSlot The hotbar slot index (0-8)
	 */
	public void swap(int rawSlot, int hotBarSlot)
	{
		int hotBarIndex = mCurrentView.getTopInventory().getSize() + 27 + hotBarSlot;
		
		if(!isPlayersInventory(getInventory(rawSlot)))
		{
			doTransaction(mCurrentView.getItem(hotBarIndex), false);
			doTransaction(mCurrentView.getItem(rawSlot), true);
		}
	}
	
	/**
	 * Drag drop items into inventory
	 * @param destinations The list of destinations and items to add 
	 */
	public void drag(List<InventorySlot> destinations)
	{
		for(InventorySlot slot : destinations)
		{
			Inventory dest = getInventory(slot.Slot);
			
			if(dest != mPickedUpInventory)
				doTransaction(slot.Item, isPlayersInventory(dest));
		}
	}
}
