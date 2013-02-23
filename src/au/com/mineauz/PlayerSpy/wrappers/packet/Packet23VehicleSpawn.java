package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.Entity;

@WrapperClass("net.minecraft.server.*.Packet23VehicleSpawn")
public class Packet23VehicleSpawn extends Packet
{
	static
	{
		initialize(Packet23VehicleSpawn.class);
	}
	
	Packet23VehicleSpawn() {}
	
	@WrapperConstructor({Entity.class, Integer.class, Integer.class})
	private static Constructor<?> mConstructor;
	
	public Packet23VehicleSpawn(Entity entity, int arg1, int arg2)
	{
		super();
		instanciate(mConstructor, entity, arg1, arg2);
	}
}
