package au.com.mineauz.PlayerSpy.wrappers.packet;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;

@WrapperClass("net.minecraft.server.*.Packet29DestroyEntity")
public class Packet29DestroyEntity extends Packet
{
	static
	{
		initialize(Packet29DestroyEntity.class);
		
		validateField(Packet29DestroyEntity.class, "a", int[].class);
	}
	
	public void setIds(int[] ids)
	{
		setFieldInstance("a", ids);
	}
}
