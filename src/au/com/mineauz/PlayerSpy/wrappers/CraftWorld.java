package au.com.mineauz.PlayerSpy.wrappers;

import java.lang.reflect.Method;


@WrapperClass("org.bukkit.craftbukkit.*.CraftWorld")
public class CraftWorld extends AutoWrapper
{
	static
	{
		initialize(CraftWorld.class);
	}
	
	private CraftWorld() {}
	
	public static CraftWorld castFrom(org.bukkit.World world)
	{
		CraftWorld wrapper = new CraftWorld();
		wrapper.mInstance = getWrappedClass(CraftWorld.class).cast(world);
		
		return wrapper;
	}
	
	@WrapperMethod(name="getHandle",returnType=World.class,parameterTypes={})
	private static Method mGetHandle;
	
	public World getHandle()
	{
		return (World) instanciateWrapper(callMethod(mGetHandle,(Object[])null));
	}
}
