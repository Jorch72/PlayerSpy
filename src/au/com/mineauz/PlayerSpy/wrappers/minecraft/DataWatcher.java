package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.DataWatcher")
public class DataWatcher extends AutoWrapper
{
	static
	{
		initialize(DataWatcher.class);
	}
	
	@WrapperMethod(name="d", returnType=boolean.class, parameterTypes={})
	private static Method mHasChanged;
	
	public boolean hasChanged()
	{
		return callMethod(mHasChanged);
	}
}
