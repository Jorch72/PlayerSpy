package au.com.mineauz.PlayerSpy;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.*;
import au.com.mineauz.PlayerSpy.Utilities.SafeChunk;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.legacy.v2.InventoryRecord;

public class RecordList extends ArrayList<Record> 
{
	private static final long serialVersionUID = 5018916041092964587L;
	
	public boolean isIndependant()
	{
		boolean inv = false, world = false, position = false;
		for(int i = 0; i < 4 && i < size(); i++)
		{
			if(get(i) instanceof IPlayerLocationAware)
			{
				if(((IPlayerLocationAware)get(i)).isFullLocation())
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
			if(record instanceof IPlayerLocationAware)
			{
				return ((IPlayerLocationAware)record).getLocation();
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
		Debug.loggedAssert( index >= 0 && index < size());
		
		Location location = null;
		for(int i = index; i >= 0; i--)
		{
			if(get(i) instanceof IPlayerLocationAware)
			{
				location = ((IPlayerLocationAware)get(i)).getLocation().clone();
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
					if(slot.Slot >= items.length)
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
		Debug.loggedAssert( splitIndex >= 0 && splitIndex < size());
		
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
			{
				Record r = new TeleportRecord(location, TeleportCause.UNKNOWN);
				r.setTimestamp(get(splitIndex).getTimestamp());
				other.add(r);
			}
			if(inventory != null)
			{
				Record r = inventory;
				r.setTimestamp(get(splitIndex).getTimestamp());
				other.add(r);
			}
			
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
	
	public List<SafeChunk> getAllChunks()
	{
		ArrayList<SafeChunk> chunks = new ArrayList<SafeChunk>();
		for(Record record : this)
		{
			SafeChunk chunk = null;
			if(record instanceof ILocationAware)
			{
				if(((ILocationAware)record).getLocation() == null)
					continue;
				
				if(((ILocationAware)record).getLocation().getWorld() == null)
					continue;
				
				chunk = new SafeChunk(((ILocationAware)record).getLocation());
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
		{
			Record r = inventory;
			r.setTimestamp(get(index).getTimestamp());
			add(0,r);
		}
		if(loc != null)
		{
			Record r = new TeleportRecord(loc, TeleportCause.UNKNOWN); 
			r.setTimestamp(get(index).getTimestamp());
			add(0,r);
		}
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
