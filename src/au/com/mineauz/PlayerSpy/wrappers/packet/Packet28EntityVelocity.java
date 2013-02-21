package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.Entity;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.Packet28EntityVelocity")
public class Packet28EntityVelocity extends Packet
{
	static
	{
		initialize(Packet28EntityVelocity.class);
	}
	
	@WrapperConstructor(Entity.class)
	private static Constructor<?> mConstructor;
	
	public Packet28EntityVelocity(Entity entity)
	{
		instanciate(mConstructor, entity);
	}
}

