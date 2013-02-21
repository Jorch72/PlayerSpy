package au.com.mineauz.PlayerSpy.wrappers;

@WrapperClass("net.minecraft.server.*.Entity")
public class Entity extends AutoWrapper
{
	static
	{
		initialize(Entity.class);
	}
}
