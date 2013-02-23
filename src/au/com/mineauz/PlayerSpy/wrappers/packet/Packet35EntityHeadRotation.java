package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.Packet35EntityHeadRotation")
public class Packet35EntityHeadRotation extends Packet
{
	static
	{
		initialize(Packet35EntityHeadRotation.class);
	}
	
	Packet35EntityHeadRotation() {}
	
	@WrapperConstructor({Integer.class, Byte.class})
	private static Constructor<?> mConstructor;
	
	public Packet35EntityHeadRotation(int entityId, byte yaw)
	{
		super();
		instanciate(mConstructor, entityId, yaw);
	}
}
