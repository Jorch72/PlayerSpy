package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.storage.StoredItemStack;

public class InventoryRecord extends Record 
{

	public InventoryRecord(ItemStack[] slots, ItemStack[] armourSlots, int heldSlot) 
	{
		super(RecordType.FullInventory);
		mItems = slots.clone();
		mArmour = armourSlots.clone();
		mSlot = heldSlot;
	}
	public InventoryRecord() 
	{
		super(RecordType.FullInventory);
	}

	public InventoryRecord(PlayerInventory inventory) 
	{
		super(RecordType.FullInventory);
		
		mItems = new ItemStack[inventory.getContents().length];
		for(int i = 0; i < inventory.getContents().length; i++)
		{
			if(inventory.getContents()[i] != null)
				mItems[i] = inventory.getContents()[i].clone();
		}

		mArmour = new ItemStack[inventory.getArmorContents().length];
		for(int i = 0; i < inventory.getArmorContents().length; i++)
		{
			if(inventory.getArmorContents()[i] != null)
				mArmour[i] = inventory.getArmorContents()[i].clone();
		}
		mSlot = inventory.getHeldItemSlot();
	}
	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeByte(mItems.length);
		for(ItemStack item : mItems)
		{
			StoredItemStack store = new StoredItemStack(item);
			store.writeItemStack(stream);
		}
		
		stream.writeByte(mArmour.length);
		for(ItemStack item : mArmour)
		{
			StoredItemStack store = new StoredItemStack(item);
			store.writeItemStack(stream);
		}
		
		stream.writeByte(mSlot);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		int itemCount = stream.readByte();
		if(itemCount <= 0 || itemCount > 36)
			throw new RecordFormatException("Bad number of item in inventory " + itemCount);
		
		mItems = new ItemStack[itemCount];
		for(int i = 0; i < mItems.length; i++)
		{
			StoredItemStack store = StoredItemStack.readItemStack(stream);
			mItems[i] = store.getItem();
			// Since null items are stored as air, make sure to make them null again
			if(mItems[i].getTypeId() == 0)
				mItems[i] = null;
		}
		
		int armourCount = stream.readByte();
		if(armourCount <= 0 || armourCount > 4)
			throw new RecordFormatException("Bad number of items in armour " + armourCount);
		
		mArmour = new ItemStack[armourCount];
		for(int i = 0; i < mArmour.length; i++)
		{
			StoredItemStack store = StoredItemStack.readItemStack(stream);
			mArmour[i] = store.getItem();
		}
		
		mSlot = stream.readByte();
	}
	
	public ItemStack[] getItems()
	{
		return mItems;
	}
	
	public ItemStack[] getArmour()
	{
		return mArmour;
	}
	
	public int getHeldSlot()
	{
		return mSlot;
	}
	
	private ItemStack[] mItems;
	private ItemStack[] mArmour;
	private int mSlot;
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		int size = 3;
		for(ItemStack item : mItems)
		{
			StoredItemStack store = new StoredItemStack(item);
			size += store.getSize();
		}
		for(ItemStack item : mArmour)
		{
			StoredItemStack store = new StoredItemStack(item);
			size += store.getSize();
		}
		return size;
	}
	@Override
	public String getDescription()
	{
		return null;
	}
	
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof InventoryRecord))
			return false;
		
		InventoryRecord record = (InventoryRecord)obj;
		
		if(mSlot != record.mSlot)
			return false;
		
		if(!Arrays.equals(mItems, record.mItems))
			return false;
		
		if(!Arrays.equals(mArmour, record.mArmour))
			return false;
		
		return true;
	}
	
	@Override
	public String toString()
	{
		return String.format("Inventory: {held: %d armour: {%s} inv: {%s}}", mSlot, Arrays.toString(mArmour), Arrays.toString(mItems)); 
	}
}
