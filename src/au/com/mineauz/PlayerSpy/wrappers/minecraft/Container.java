package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.Container")
public class Container extends Item
{
	static
	{
		initialize(Container.class);
	}
	
	@WrapperMethod(name="getSlot", returnType=Slot.class, parameterTypes=int.class)
	private static Method mGetSlot;
	
	public Slot getSlot(int index)
	{
		return callMethod(mGetSlot, index);
	}
}
