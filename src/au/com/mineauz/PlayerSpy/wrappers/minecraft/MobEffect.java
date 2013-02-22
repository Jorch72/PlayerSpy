package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.MobEffect")
public class MobEffect extends AutoWrapper
{
	static
	{
		initialize(MobEffect.class);
	}
	
	@WrapperMethod(name="f", returnType=String.class, parameterTypes={})
	private static Method mGetEffectName;
	
	public String getEffectName()
	{
		return callMethod(mGetEffectName);
	}
}
