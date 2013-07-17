package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.bukkit.inventory.Inventory;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;
import au.com.mineauz.PlayerSpy.wrappers.craftbukkit.CraftItemStack;
import au.com.mineauz.PlayerSpy.wrappers.nbt.NBTTagList;

@WrapperClass("net.minecraft.server.*.InventoryEnderChest")
public class InventoryEnderChest extends AutoWrapper
{
	static
	{
		initialize(InventoryEnderChest.class);
	}
	
	@WrapperConstructor()
	private static Constructor<?> mConstructor;
	
	public InventoryEnderChest()
	{
		super();
		instanciate(mConstructor);
	}
	
	@WrapperMethod(name="getContents", returnType=ItemStack[].class, parameterTypes={})
	private static Method mGetContents;
	
	public ItemStack[] getContents()
	{
		return callMethod(mGetContents);
	}

	@WrapperMethod(name="a", returnType=void.class, parameterTypes=NBTTagList.class)
	private static Method mReadFromNBT;
	public void readFromNBT( NBTTagList items )
	{
		callMethod(mReadFromNBT, items);
	}
	
	@WrapperMethod(name="h", returnType=NBTTagList.class, parameterTypes={})
	private static Method mWriteToNBT;
	public NBTTagList writeToNBT()
	{
		return callMethod(mWriteToNBT);
	}
	
	public static InventoryEnderChest from(Inventory inventory)
	{
		InventoryEnderChest inv = new InventoryEnderChest();
		try
		{
			Object[] items = (Object[]) mGetContents.invoke(inv.mInstance);
			for(int i = 0; i < 27; ++i)
				items[i] = AutoWrapper.unwrapObjects(CraftItemStack.asNMSCopy(inventory.getItem(i)));
			
			return inv;
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			return null;
		}
	}
	public static InventoryEnderChest castFrom(Object iinventory)
	{
		InventoryEnderChest inv = new InventoryEnderChest();
		inv.mInstance = getWrappedClass(InventoryEnderChest.class).cast(iinventory);
		
		return inv;
	}
}
