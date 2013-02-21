package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.Entity;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.Packet34EntityTeleport")
public class Packet34EntityTeleport extends Packet
{
	static
	{
		initialize(Packet34EntityTeleport.class);
	}
	
	@WrapperConstructor(Entity.class)
	private static Constructor<?> mConstructor;
	
	public Packet34EntityTeleport(Entity entity)
	{
		instanciate(mConstructor, entity);
	}
}
