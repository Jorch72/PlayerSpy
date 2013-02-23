package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.FieldWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;
import au.com.mineauz.PlayerSpy.wrappers.WrapperField;

@WrapperClass("net.minecraft.server.*.Packet29DestroyEntity")
public class Packet29DestroyEntity extends Packet
{
	static
	{
		initialize(Packet29DestroyEntity.class);
	}
	
	public Packet29DestroyEntity()
	{
		instanciate();
	}
	
	@WrapperConstructor(int[].class)
	private static Constructor<?> mConstructor;
	
	public Packet29DestroyEntity(int... id)
	{
		instanciate(mConstructor, id);
	}
	
	@WrapperField(name="a", type=int[].class)
	public FieldWrapper<int[]> ids;
}
