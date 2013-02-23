package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.Entity;

@WrapperClass("net.minecraft.server.*.Packet18ArmAnimation")
public class Packet18ArmAnimation extends Packet
{
	static
	{
		initialize(Packet18ArmAnimation.class);
	}
	
	Packet18ArmAnimation() {}
	
	@WrapperConstructor({Entity.class, Integer.class})
	private static Constructor<?> mConstructor;
	
	public Packet18ArmAnimation(Entity entity, int animation)
	{
		super();
		instanciate(mConstructor, entity, animation);
	}
}
