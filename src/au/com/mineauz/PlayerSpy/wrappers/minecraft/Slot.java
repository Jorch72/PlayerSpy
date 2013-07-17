package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.Slot")
public class Slot extends AutoWrapper
{
	static
	{
		initialize(Slot.class);
	}
	
	Slot() {}
	
	@WrapperMethod(name="isAllowed", returnType=boolean.class, parameterTypes=ItemStack.class)
	private static Method mIsAllowed;
	
	public boolean isAllowed(ItemStack stack)
	{
		return callMethod(mIsAllowed, stack);
	}
}
