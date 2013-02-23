package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.FieldWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;
import au.com.mineauz.PlayerSpy.wrappers.WrapperField;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.PlayerInventory")
public class PlayerInventory extends AutoWrapper
{
	static
	{
		initialize(PlayerInventory.class);
		
		validateField(PlayerInventory.class, "items", ItemStack[].class);
		validateField(PlayerInventory.class, "armor", ItemStack[].class);
	}
	
	PlayerInventory() {}
	
	@WrapperConstructor(EntityHuman.class)
	private static Constructor<?> mConstructor;
	
	public PlayerInventory(EntityHuman player)
	{
		super();
		instanciate(mConstructor, player);
	}
	
	@WrapperField(name="itemInHandIndex", type=Integer.class)
	public FieldWrapper<Integer> itemInHandIndex;
	
	@WrapperField(name="e", type=Boolean.class)
	public FieldWrapper<Boolean> hasChanged;
	
	@WrapperMethod(name="update", returnType=Void.class, parameterTypes={})
	private static Method mUpdate;
	
	public void update()
	{
		callMethod(mUpdate);
	}
	
	public int size()
	{
		Object[] items = getFieldInstance("items");
		return items.length;
	}
	
	public void setItem(int index, ItemStack item)
	{
		Object[] items = getFieldInstance("items");
		
		if(item == null)
			items[index] = null;
		else
			items[index] = item.getNativeInstance();
	}
	
	public void setArmor(int index, ItemStack item)
	{
		Object[] items = getFieldInstance("armor");
		
		if(item == null)
			items[index] = null;
		else
			items[index] = item.getNativeInstance();
	}
	
	public ItemStack getItem(int index)
	{
		Object[] items = getFieldInstance("items");
		
		return (ItemStack)instanciateWrapper(items[index]);
	}
	
	public ItemStack getArmor(int index)
	{
		Object[] items = getFieldInstance("armor");
		
		return (ItemStack)instanciateWrapper(items[index]);
	}
	
	public void clear()
	{
		Object[] items = getFieldInstance("items");
		Object[] armor = getFieldInstance("armor");
		
		Arrays.fill(items, null);
		Arrays.fill(armor, null);
	}
}
