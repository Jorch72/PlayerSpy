package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.Packet62NamedSoundEffect")
public class Packet62NamedSoundEffect extends Packet
{
	static
	{
		initialize(Packet62NamedSoundEffect.class);
	}
	
	Packet62NamedSoundEffect() {}
	
	@WrapperConstructor({String.class, Double.class, Double.class, Double.class, Float.class, Float.class})
	private static Constructor<?> mConstructor;
	
	public Packet62NamedSoundEffect(String name, double x, double y, double z, float volume, float pitch)
	{
		super();
		instanciate(mConstructor, name, x, y, z, volume, pitch);
	}
	
}
