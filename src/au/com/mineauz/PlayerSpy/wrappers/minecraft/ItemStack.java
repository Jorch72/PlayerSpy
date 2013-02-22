package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.ItemStack")
public class ItemStack extends AutoWrapper
{
	static
	{
		initialize(ItemStack.class);
	}
	
	protected ItemStack() {}
	
	@WrapperConstructor({Item.class, Integer.class, Integer.class})
	private static Constructor<?> mConstructor;
	
	public ItemStack(Item item, int amount, int data)
	{
		instanciate(mConstructor, item, amount, data);
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
}
