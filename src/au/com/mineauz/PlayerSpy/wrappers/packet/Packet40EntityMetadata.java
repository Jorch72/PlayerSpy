package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.DataWatcher;

@WrapperClass("net.minecraft.server.*.Packet40EntityMetadata")
public class Packet40EntityMetadata extends Packet
{
	static
	{
		initialize(Packet40EntityMetadata.class);
	}
	
	Packet40EntityMetadata() {}
	
	@WrapperConstructor({Integer.class, DataWatcher.class, Boolean.class})
	private static Constructor<?> mConstructor;
	
	public Packet40EntityMetadata(int entityId, DataWatcher dw, boolean value)
	{
		super();
		instanciate(mConstructor, entityId, dw, value);
	}
			
}
