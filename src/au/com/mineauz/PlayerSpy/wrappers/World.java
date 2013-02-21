package au.com.mineauz.PlayerSpy.wrappers;

@WrapperClass("net.minecraft.server.*.World")
public class World extends AutoWrapper
{
	static
	{
		initialize(World.class);
	}
}
