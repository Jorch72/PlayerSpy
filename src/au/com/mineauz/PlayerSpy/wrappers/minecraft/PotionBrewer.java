package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.FieldWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperField;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.PotionBrewer")
public class PotionBrewer extends AutoWrapper
{
	static
	{
		initialize(PotionBrewer.class);
	}
	
	@WrapperMethod(name="a",returnType=Boolean.class, parameterTypes={Integer.class, Integer.class})
	private static Method mCheckFlag;
	
	public static boolean checkFlag(int var1, int var2)
	{
		return callStaticMethod(mCheckFlag, var1, var2);
	}
	
	@WrapperField(name="appearances", type=String[].class)
	public static FieldWrapper<String[]> potionPrefixes;
}
