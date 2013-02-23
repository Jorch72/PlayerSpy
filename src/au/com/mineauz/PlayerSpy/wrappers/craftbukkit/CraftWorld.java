package au.com.mineauz.PlayerSpy.wrappers.craftbukkit;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.WorldServer;


@WrapperClass("org.bukkit.craftbukkit.*.CraftWorld")
public class CraftWorld extends AutoWrapper
{
	static
	{
		initialize(CraftWorld.class);
	}
	
	CraftWorld() {}
	
	public static CraftWorld castFrom(org.bukkit.World world)
	{
		CraftWorld wrapper = new CraftWorld();
		wrapper.mInstance = getWrappedClass(CraftWorld.class).cast(world);
		
		return wrapper;
	}
	
	@WrapperMethod(name="getHandle",returnType=WorldServer.class,parameterTypes={})
	private static Method mGetHandle;
	
	public WorldServer getHandle()
	{
		return callMethod(mGetHandle,(Object[])null);
	}
}
