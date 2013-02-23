package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.Entity;

@WrapperClass("net.minecraft.server.*.Packet28EntityVelocity")
public class Packet28EntityVelocity extends Packet
{
	static
	{
		initialize(Packet28EntityVelocity.class);
	}
	
	Packet28EntityVelocity() {}
	
	@WrapperConstructor(Entity.class)
	private static Constructor<?> mConstructor;
	
	public Packet28EntityVelocity(Entity entity)
	{
		super();
		instanciate(mConstructor, entity);
	}
}

