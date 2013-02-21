package au.com.mineauz.PlayerSpy.wrappers;

import java.lang.reflect.Method;

@WrapperClass("org.bukkit.craftbukkit.*.entity.CraftPlayer")
public class CraftPlayer extends AutoWrapper
{
	static
	{
		initialize(CraftPlayer.class);
	}
	
	public static CraftPlayer castFrom(org.bukkit.entity.Player player)
	{
		CraftPlayer wrapper = new CraftPlayer();
		wrapper.mInstance = getWrappedClass(CraftPlayer.class).cast(player);
		
		return wrapper;
	}
	
	@WrapperMethod(name="getHandle",returnType=EntityPlayer.class,parameterTypes={})
	private static Method mGetHandle;
	
	public EntityPlayer getHandle()
	{
		return (EntityPlayer) instanciateWrapper(callMethod(mGetHandle, (Object[])null));
	}
}
