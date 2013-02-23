package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;
import java.util.List;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.ItemPotion")
public class ItemPotion extends Item
{
	static
	{
		initialize(ItemPotion.class);
	}
	
	@WrapperMethod(name="g", returnType=Boolean.class, parameterTypes=Integer.class)
	private static Method mIsSplash;
	
	public static boolean isSplash(int id)
	{
		return callStaticMethod(mIsSplash, id);
	}
	
	@WrapperMethod(name="g", returnType=List.class, parameterTypes=ItemStack.class)
	private static Method mGetEffects;
	public List<?> getEffects(ItemStack stack)
	{
		return callMethod(mGetEffects, stack);
	}
}
