package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.*;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.EntityHuman;

@WrapperClass("net.minecraft.server.*.Packet20NamedEntitySpawn")
public class Packet20NamedEntitySpawn extends Packet
{
	static
	{
		initialize(Packet20NamedEntitySpawn.class);
	}
	
	@WrapperConstructor(EntityHuman.class)
	private static Constructor<?> mConstructor;
	
	public Packet20NamedEntitySpawn(EntityHuman human)
	{
		instanciate(mConstructor, human);
	}
	
	@WrapperField(name="b", type=String.class)
	public FieldWrapper<String> name;
}
