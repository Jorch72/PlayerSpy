package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.EntityLiving;

@WrapperClass("net.minecraft.server.*.Packet24MobSpawn")
public class Packet24MobSpawn extends Packet
{
	static
	{
		initialize(Packet24MobSpawn.class);
	}
	
	Packet24MobSpawn() {}
	
	@WrapperConstructor(EntityLiving.class)
	private static Constructor<?> mConstructor;
	
	public Packet24MobSpawn(EntityLiving entity)
	{
		super();
		instanciate(mConstructor, entity);
	}
}
