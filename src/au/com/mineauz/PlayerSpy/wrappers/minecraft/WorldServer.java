package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;

@WrapperClass("net.minecraft.server.*.WorldServer")
public class WorldServer extends World
{
	static
	{
		initialize(WorldServer.class);
	}
}
