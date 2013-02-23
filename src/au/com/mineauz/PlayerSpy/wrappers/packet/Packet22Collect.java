package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.Packet22Collect")
public class Packet22Collect extends Packet
{
	static
	{
		initialize(Packet22Collect.class);
	}
	
	Packet22Collect() {}
	
	@WrapperConstructor({Integer.class, Integer.class})
	private static Constructor<?> mConstructor;
	
	public Packet22Collect(int itemId, int entityId)
	{
		super();
		instanciate(mConstructor, itemId, entityId);
	}
}
