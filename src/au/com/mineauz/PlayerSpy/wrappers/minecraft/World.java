package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;
import au.com.mineauz.PlayerSpy.wrappers.craftbukkit.CraftWorld;

@WrapperClass("net.minecraft.server.*.World")
public class World extends AutoWrapper
{
	static
	{
		initialize(World.class);
	}

	@WrapperMethod(name="getWorld", returnType=CraftWorld.class, parameterTypes={})
	private static Method mGetWorld;
	
	public org.bukkit.World getWorld()
	{
		return (org.bukkit.World)((CraftWorld)callMethod(mGetWorld, (Object[])null)).getNativeInstance();
	}
}
