package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;
import au.com.mineauz.PlayerSpy.wrappers.packet.Packet;

@WrapperClass("net.minecraft.server.*.PlayerConnection")
public class PlayerConnection extends AutoWrapper
{
	static
	{
		initialize(PlayerConnection.class);
	}
	
	@WrapperMethod(name="sendPacket",returnType=Void.class,parameterTypes=Packet.class)
	private static Method mSendPacket;
	
	public void sendPacket(Packet packet)
	{
		callMethod(mSendPacket, packet);
	}
}
