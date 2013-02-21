package au.com.mineauz.PlayerSpy.wrappers;

@WrapperClass("net.minecraft.server.*.EntityHuman")
public class EntityHuman extends AutoWrapper
{
	static
	{
		initialize(EntityHuman.class);
	}
}
