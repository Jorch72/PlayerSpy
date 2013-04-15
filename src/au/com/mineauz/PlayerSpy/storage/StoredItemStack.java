package au.com.mineauz.PlayerSpy.storage;

import java.io.*;

import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;

public class StoredItemStack 
{
	public StoredItemStack(ItemStack stack)
	{
		if(stack != null)
			mItem = stack.clone();
		else
			mItem = new ItemStack(0);
	}
	
	public ItemStack getItem()
	{
		return mItem;
	}
	
	public void writeItemStack(DataOutputStream stream) throws IOException
	{
		// item id
		stream.writeInt(mItem.getTypeId());
		// item durability
		stream.writeShort(mItem.getDurability());
		// item count
		stream.writeByte(mItem.getAmount());
		
		// item metadata
		StoredItemMeta meta = new StoredItemMeta(mItem.getItemMeta());
		meta.write(stream);

	}
	
	public static StoredItemStack readItemStack(DataInputStream stream) throws IOException, RecordFormatException
	{
		int itemId, amount;
		short durability;
		
		itemId = stream.readInt();
		durability = stream.readShort();
		amount = stream.readByte();
		
		if(itemId < 0 || itemId > Short.MAX_VALUE)
			throw new RecordFormatException("Bad item id " + itemId);
		
		if(amount < 0)
			throw new RecordFormatException("Bad amount " + amount);
		
		ItemStack item = new ItemStack(itemId,amount,durability);
		
		StoredItemMeta meta = new StoredItemMeta();
		meta.read(stream);
		item.setItemMeta(meta.getMeta());
		
		return new StoredItemStack(item);
	}
	
	public int getSize()
	{
		StoredItemMeta meta = new StoredItemMeta(mItem.getItemMeta());
		
		return 7 + meta.getSize();
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof StoredItemStack))
			return false;
		
		StoredItemStack stack = (StoredItemStack)obj;
		
		return mItem.equals(stack.mItem);
	}
	
	private ItemStack mItem;
}
