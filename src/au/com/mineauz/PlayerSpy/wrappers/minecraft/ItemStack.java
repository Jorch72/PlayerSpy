package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.FieldWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;
import au.com.mineauz.PlayerSpy.wrappers.WrapperField;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;
import au.com.mineauz.PlayerSpy.wrappers.nbt.NBTTagCompound;

@WrapperClass("net.minecraft.server.*.ItemStack")
public class ItemStack extends AutoWrapper
{
	static
	{
		initialize(ItemStack.class);
	}

	@WrapperField(name="id", type=Integer.class)
	public FieldWrapper<Integer> id;
	
	ItemStack() {}
	
	@WrapperConstructor({Item.class, Integer.class, Integer.class})
	private static Constructor<?> mConstructor;
	
	public ItemStack(Item item, int amount, int data)
	{
		super();
		instanciate(mConstructor, item, amount, data);
	}
	
	@WrapperConstructor({int.class, int.class, int.class})
	private static Constructor<?> mConstructor2;
	public ItemStack(int type, int amount, int data)
	{
		super();
		instanciate(mConstructor2, type, amount, data);
	}
	
	@WrapperMethod(name="getItem", returnType=Item.class, parameterTypes={})
	private static Method mGetItem;
	
	public Item getItem()
	{
		return callMethod(mGetItem);
	}
	
	@WrapperMethod(name="getData", returnType=Integer.class)
	private static Method mGetData;
	
	public int getData()
	{
		return callMethod(mGetData);
	}
	
	@WrapperMethod(name="c", returnType=void.class, parameterTypes=NBTTagCompound.class)
	private static Method mReadFromNBT;
	public void readFromNBT(NBTTagCompound root)
	{
		callMethod(mReadFromNBT,root);
	}
	
	@WrapperMethod(name="save", returnType=NBTTagCompound.class, parameterTypes=NBTTagCompound.class)
	private static Method mWriteToNBT;
	public NBTTagCompound writeToNBT(NBTTagCompound root)
	{
		return callMethod(mWriteToNBT, root);
	}
}
