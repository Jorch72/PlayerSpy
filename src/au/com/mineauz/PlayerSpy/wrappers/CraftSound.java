package au.com.mineauz.PlayerSpy.wrappers;

import java.lang.reflect.Method;

import org.bukkit.Sound;

@WrapperClass("org.bukkit.craftbukkit.*.CraftSound")
public class CraftSound extends AutoWrapper
{
	static
	{
		initialize(CraftSound.class);
	}
	
	private CraftSound() {}
	
	@WrapperMethod(name="getSound",returnType=String.class, parameterTypes=Sound.class)
	private static Method mGetSound;
	
	public static String getSound(Sound sound)
	{
		return callStaticMethod(mGetSound, sound);
	}
}
