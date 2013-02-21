package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.*;

@WrapperClass("net.minecraft.server.*.Packet20NamedEntitySpawn")
public class Packet20NamedEntitySpawn extends Packet
{
	static
	{
		initialize(Packet20NamedEntitySpawn.class);
		
		validateField(Packet20NamedEntitySpawn.class, "b", String.class);
	}
	
	@WrapperConstructor(EntityHuman.class)
	private static Constructor<?> mConstructor;
	
	public Packet20NamedEntitySpawn(EntityHuman human)
	{
		instanciate(mConstructor, human);
	}
	
	public String getName()
	{
		return getFieldInstance("b");
	}
	
	public void setName(String name)
	{
		setFieldInstance("b", name);
	}
}
