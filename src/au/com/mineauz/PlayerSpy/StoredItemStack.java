package au.com.mineauz.PlayerSpy;

import java.io.*;
import java.util.HashMap;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

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
	
	public boolean writeItemStack(DataOutputStream stream)
	{
		try
		{
			// item id
			stream.writeInt(mItem.getTypeId());
			// item data
			stream.writeByte(mItem.getData().getData());
			// item durability
			stream.writeShort(mItem.getDurability());
			// item count
			stream.writeByte(mItem.getAmount());
			// has enchants
			stream.writeByte(mItem.getEnchantments().size());
			
			for(Enchantment ench : mItem.getEnchantments().keySet())
			{
				stream.writeShort(ench.getId());
				stream.writeShort(mItem.getEnchantments().get(ench));
			}
			
			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
	
	public static StoredItemStack readItemStack(DataInputStream stream)
	{
		try
		{
			int itemId, amount;
			short durability;
			byte data;
			
			itemId = stream.readInt();
			data = stream.readByte();
			durability = stream.readShort();
			amount = stream.readByte();
			int enchantCount = stream.readByte();
			
			HashMap<Enchantment,Integer> map = new HashMap<Enchantment,Integer>();
			
			for(int i = 0; i < enchantCount; i++)
			{
				Enchantment ench = Enchantment.getById(stream.readShort());
				map.put(ench,(int)stream.readShort());
			}
			ItemStack item = new ItemStack(itemId,amount,durability,data);
			item.addUnsafeEnchantments(map);
			
			return new StoredItemStack(item);
		}
		catch(IOException e)
		{
			return null;
		}
	}
	
	public int getSize()
	{
		return 9 + 4 * mItem.getEnchantments().size();
	}
	private ItemStack mItem;
}
