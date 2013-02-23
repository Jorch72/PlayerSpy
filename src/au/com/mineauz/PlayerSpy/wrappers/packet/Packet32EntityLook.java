package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.Packet32EntityLook")
public class Packet32EntityLook extends Packet
{
	static
	{
		initialize(Packet32EntityLook.class);
	}
	
	Packet32EntityLook() {}
	
	@WrapperConstructor({Integer.class, Byte.class, Byte.class})
	private static Constructor<?> mConstructor;
	
	public Packet32EntityLook(int entityId, byte yaw, byte pitch)
	{
		super();
		instanciate(mConstructor, entityId, yaw, pitch);
	}
}
