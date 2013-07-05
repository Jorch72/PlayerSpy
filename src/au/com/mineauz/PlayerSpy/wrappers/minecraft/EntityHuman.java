package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.FieldWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperField;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.EntityHuman")
public class EntityHuman extends EntityLiving
{
	static
	{
		initialize(EntityHuman.class);
	}
	
	@WrapperMethod(name="getName", parameterTypes={}, returnType=String.class)
	private static Method mGetName;
	
	public String getName()
	{
		return callMethod(mGetName);
	}
	
	@WrapperField(name="sleeping", type=boolean.class)
	public FieldWrapper<Boolean> sleeping; 
	
	@WrapperField(name="inventory", type=PlayerInventory.class)
	public FieldWrapper<PlayerInventory> inventory;
}
