package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.FieldWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperField;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.Item")
public class Item extends AutoWrapper
{
	static
	{
		initialize(Item.class);
	}
	
	Item() {}
	
	@WrapperField(name="id", type=int.class)
	public FieldWrapper<Integer> id;
	
	@WrapperField(name="byId", type=Item[].class)
	public static FieldWrapper<Item[]> byId;
	
	@WrapperField(name="POTION", type=ItemPotion.class)
	public static FieldWrapper<ItemPotion> POTION;
	
	@WrapperMethod(name="d", returnType=String.class, parameterTypes=ItemStack.class)
	private static Method mGetItemNameIS;
	
	public String getItemNameIS(ItemStack stack)
	{
		return callMethod(mGetItemNameIS, stack);
	}
	
	@WrapperMethod(name="getName", returnType=String.class, parameterTypes={})
	private static Method mGetName;
	
	public String getName()
	{
		return callMethod(mGetName);
	}
	
	@WrapperMethod(name="m", returnType=boolean.class, parameterTypes={})
	private static Method mHasSubtypes;
	
	public boolean hasSubtypes()
	{
		return callMethod(mHasSubtypes);
	}
	
	
}
