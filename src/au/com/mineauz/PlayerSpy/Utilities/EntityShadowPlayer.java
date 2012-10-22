package au.com.mineauz.PlayerSpy.Utilities;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.World;

public class EntityShadowPlayer extends EntityHuman
{
	public EntityShadowPlayer(World world, String name) 
	{
		super(world);
		this.name = name;
	}

	@Override
	public int getMaxHealth() 
	{
		return 20;
	}

	@Override
	public boolean b(String arg0) 
	{
		this.name = arg0;
		return false;
	}

	@Override
	public void sendMessage(String arg0) 
	{
		// Do Nothing
	}

}
