package au.com.mineauz.PlayerSpy.wrappers.packet;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;

@WrapperClass("net.minecraft.server.*.Packet")
public class Packet extends AutoWrapper
{
	static
	{
		initialize(Packet.class);
	}
}
