package au.com.mineauz.PlayerSpy.legacy.v2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.legacy.StoredItemStack;

@Deprecated
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
}
