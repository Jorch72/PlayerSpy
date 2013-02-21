package au.com.mineauz.PlayerSpy.wrappers;

@WrapperClass("net.minecraft.server.*.EntityPlayer")
public class EntityPlayer extends EntityHuman
{
	static
	{
		initialize(EntityPlayer.class);
		
		validateField(EntityPlayer.class, "playerConnection", PlayerConnection.class);
	}
	
	public PlayerConnection getPlayerConnection()
	{
		return (PlayerConnection)instanciateWrapper(getFieldInstance("playerConnection"));
	}
}
