package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.DataWatcher;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.Packet40EntityMetadata")
public class Packet40EntityMetadata extends Packet
{
	static
	{
		initialize(Packet40EntityMetadata.class);
	}
	
	@WrapperConstructor({Integer.class, DataWatcher.class, Boolean.class})
	private static Constructor<?> mConstructor;
	
	public Packet40EntityMetadata(int entityId, DataWatcher dw, boolean value)
	{
		instanciate(mConstructor, entityId, dw, value);
	}
			
}
