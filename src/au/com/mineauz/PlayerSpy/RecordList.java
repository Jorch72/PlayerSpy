package au.com.mineauz.PlayerSpy;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.*;

public class RecordList extends ArrayList<Record> 
{
	private static final long serialVersionUID = 5018916041092964587L;
	
	public boolean isIndependant()
	{
		boolean inv = false, world = false, position = false;
		for(int i = 0; i < 4 && i < size(); i++)
		{
			if(get(i) instanceof ILocationAware)
			{
				if(((ILocationAware)get(i)).isFullLocation())
					world = true;
				position = true;
			}
			
			switch(get(i).getType())
			{
			case FullInventory:
				inv = true;
				break;
			default:
			}
		}
		
		if(!inv || !position || !world)
			return false;
		else
			return true;
	}
	public Location getFirstLocation()
	{
		for(Record record : this)
		{
			if(record instanceof ILocationAware)
			{
				return ((ILocationAware)record).getLocation();
			}
		}
		return null;
	}
	public InventoryRecord getFirstInventory()
	{
		for(Record record : this)
		{
			if(record instanceof InventoryRecord)
			{
				return (InventoryRecord)record;
			}
		}
		
		return null;
	}
	public int getNextRecordAfter(long date)
	{
		int i = 0;
		for(Record record : this)
		{
			if(record.getTimestamp() > date)
				return i;
			
			i++;
		}
		
		return i;
	}
	public int getLastRecordBefore(long date)
	{
		for(int i = this.size()-1; i >= 0; i--)
		{
			if(this.get(i).getTimestamp() < date)
				return i;
		}
		
		return -1;
	}
	
	/// gets the current location at that index
	public Location getCurrentLocation(int index)
	{
		assert index >= 0 && index < size();
		
		Location location = null;
		for(int i = index; i >= 0; i--)
		{
			if(get(i) instanceof ILocationAware)
			{
				location = ((ILocationAware)get(i)).getLocation().clone();
				break;
			}
		}
		
		return location;
	}
	
	/// gets the current state of the inventory at that index
	public InventoryRecord getCurrentInventory(int index)
	{
		ItemStack[] items = null;
		ItemStack[] armour = null;
		int held = 0;
		
		if(index >= size())
			return null;
		int i;
		for(i = index; i >= 0; i--)
		{
			if(get(i).getType() == RecordType.FullInventory)
			{
				items = ((InventoryRecord)get(i)).getItems();
				armour = ((InventoryRecord)get(i)).getArmour();
				held = ((InventoryRecord)get(i)).getHeldSlot();
				
				break;
			}
		}
		
		// Check if there was a record at all
		if(items == null)
			return null;
		
		// Now apply updates
		for(; i < index; i++)
		{
			switch(get(i).getType())
			{
			case UpdateInventory:
			{
				UpdateInventoryRecord urecord = (UpdateInventoryRecord)get(i);
				
				for(InventorySlot slot : urecord.Slots)
				{
					if(slot.Slot > items.length)
						armour[slot.Slot - items.length] = (slot.Item == null ? null : slot.Item.clone());
					else
						items[slot.Slot] = (slot.Item == null ? null : slot.Item.clone()); 
				}
				break;
			}
			case HeldItemChange:
				held = ((HeldItemChangeRecord)get(i)).getSlot();
				break;
			default:
				break;
			}
		}
		
		return new InventoryRecord(items,armour,held); 
	}
	/// Splits the list of records in a way that will keep a consistent state
	/// if keep lower is set, every record < splitIndex, will be kept in this list, and the rest will be returned.
	/// if keep lower is not set, every record >= splitIndex, will be kept in this list, and the rest will be returned.
	public RecordList splitRecords(int splitIndex, boolean keepLower)
	{
		assert splitIndex >= 0 && splitIndex < size();
		
		// Handle cases where there is no need for splitting
		if(splitIndex == 0)
		{
			if(keepLower)
			{
				// Clear this list and return what was there
				RecordList other = (RecordList)this.clone();
				clear();
				return other;
			}
			else
			{
				// return an empty list
				return new RecordList();
			}
		}
		else if(splitIndex == size() - 1)
		{
			if(keepLower)
				// Return an empty list
				return new RecordList();
			else
			{
				// Clear this list and return what was there
				RecordList other = (RecordList)this.clone();
				clear();
				return other;
			}
		}
		
		
		RecordList other = new RecordList();

		InventoryRecord inventory = getCurrentInventory(splitIndex);
		Location location = getCurrentLocation(splitIndex);
		
		// Copy records to other, and remove them from this one
		if(keepLower)
		{
			other.ensureCapacity(size() - splitIndex);
			
			// ensure state is consistent
			if(location != null)
				other.add(new TeleportRecord(location, TeleportCause.UNKNOWN));
			if(inventory != null)
				other.add(inventory);
			
			// Copy records
			for(int i = splitIndex; i < size(); i++)
				other.add(get(i));
			
			// Remove them from this list
			removeRange(splitIndex, size());
		}
		else
		{
			other.ensureCapacity(splitIndex);
			
			// Copy records
			for(int i = 0; i < splitIndex; i++)
				other.add(get(i));
			
			// Removes them from this list
			removeRange(0,splitIndex);
			
			// ensure state is consistant
			if(inventory != null)
				add(0,inventory);
			if(location != null)
				add(0,new TeleportRecord(location, TeleportCause.UNKNOWN));
		}
		
		return other;
	}

	/**
	 * Gets the timestamp of the first record
	 */
	public long getStartTimestamp()
	{
		if(size() == 0)
			return 0;
		return get(0).getTimestamp();
	}
	
	/**
	 * Gets the timestamp of the last record
	 */
	public long getEndTimestamp()
	{
		if(size() == 0)
			return 0;
		return get(size()-1).getTimestamp();
	}
	
	public List<Chunk> getAllChunks()
	{
		ArrayList<Chunk> chunks = new ArrayList<Chunk>();
		for(Record record : this)
		{
			Chunk chunk = null;
			if(record instanceof ILocationAware)
			{
				if(((ILocationAware)record).getLocation().getWorld() == null)
					LogUtil.info("Null world found" + record.toString());
				chunk = ((ILocationAware)record).getLocation().getChunk();
			}
			else if(record instanceof BlockChangeRecord)
				chunk = ((BlockChangeRecord)record).getInitialBlock().getLocation().getChunk();
			else if(record instanceof AttackRecord)
				chunk = ((AttackRecord)record).getDamagee().getLocation().getChunk();
			else if(record instanceof DamageRecord)
			{
				if(((DamageRecord)record).getDamager() == null)
					continue;
				chunk = ((DamageRecord)record).getDamager().getLocation().getChunk();
			}
			else if(record instanceof InteractRecord)
			{
				if(((InteractRecord)record).getBlock() != null)
					chunk = ((InteractRecord)record).getBlock().getLocation().getChunk();
				else if(((InteractRecord)record).getEntity() != null)
					chunk = ((InteractRecord)record).getEntity().getLocation().getChunk();
				else
					continue;
			}
			else if(record instanceof ItemPickupRecord)
				chunk = ((ItemPickupRecord)record).getLocation().getChunk();
			else if(record instanceof RightClickActionRecord)
			{
				if(((RightClickActionRecord)record).getEntity() != null)
					chunk = ((RightClickActionRecord)record).getEntity().getLocation().getChunk();
			}
			else if(record instanceof SleepRecord)
				chunk = ((SleepRecord)record).getBedLocation().getChunk();
			else if(record instanceof VehicleMountRecord)
				chunk = ((VehicleMountRecord)record).getVehicle().getLocation().getChunk();
			else if(record instanceof InventoryTransactionRecord)
			{
				if(((InventoryTransactionRecord)record).getInventoryInfo().getBlock() != null)
					chunk = ((InventoryTransactionRecord)record).getInventoryInfo().getBlock().getLocation().getChunk();
				else if(((InventoryTransactionRecord)record).getInventoryInfo().getEntity() != null)
					chunk = ((InventoryTransactionRecord)record).getInventoryInfo().getEntity().getLocation().getChunk();
				else
					continue;
			}
			else
				continue;
			
			if(!chunks.contains(chunk))
				chunks.add(chunk);
		}
		
		return chunks;
	}
	/**
	 * Removes all records before the index
	 * @param index The index to remove items before exclusive of it
	 */
	public void removeBefore(int index)
	{
		// Ensure state
		Location loc = getCurrentLocation(index);
		InventoryRecord inventory = getCurrentInventory(index);
		
		removeRange(0, index);
		
		// Ensure state
		if(inventory != null)
			add(0,inventory);
		if(loc != null)
			add(0,new TeleportRecord(loc, TeleportCause.UNKNOWN));
	}
	
	public long getDataSize(boolean absolute)
	{
		long size = 0;
		for(Record r : this)
			size += r.getSize(absolute);
		
		return size;
	}
	
	public boolean getDeep()
	{
		for(Record record : this)
		{
			if(record instanceof SessionInfoRecord)
			{
				return ((SessionInfoRecord)record).isDeep();
			}
		}
		
		return false;
	}
}
