package au.com.mineauz.PlayerSpy.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;

public class InventorySlot 
{
	public ItemStack Item;
	public int Slot;
	public InventorySlot()
	{
		
	}
	public InventorySlot(ItemStack item, int slot)
	{
		Item = (item == null ? null : item.clone());
		Slot = slot;
	}
	public void write(DataOutputStream stream) throws IOException
	{
		stream.writeShort((short)Slot);
		new StoredItemStack(Item).writeItemStack(stream);
	}
	
	public static InventorySlot read(DataInputStream stream) throws IOException, RecordFormatException
	{
		InventorySlot slot = new InventorySlot();
		slot.Slot = stream.readShort();
		
		slot.Item = StoredItemStack.readItemStack(stream).getItem();

		if(slot.Item.getTypeId() == 0)
			slot.Item = null;
		
		return slot;
	}
	
	public int getSize()
	{
		return 2 + new StoredItemStack(Item).getSize();
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof InventorySlot))
			return false;
		
		InventorySlot slot = (InventorySlot)obj;
		
		if(Slot != slot.Slot)
			return false;
		
		if(Item != null)
			return Item.equals(slot.Item);
		else
			return slot.Item == null;

	}
	
	@Override
	public String toString()
	{
		return "InventorySlot: { slot: " + Slot + " item: " + Item + " }";
	}
}
