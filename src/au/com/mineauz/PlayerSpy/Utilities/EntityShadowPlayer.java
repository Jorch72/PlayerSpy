package au.com.mineauz.PlayerSpy.Utilities;

import net.minecraft.server.v1_4_R1.ChunkCoordinates;
import net.minecraft.server.v1_4_R1.EntityHuman;
import net.minecraft.server.v1_4_R1.World;

// TODO: Make this class dynamic
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
	public void b(String arg0) 
	{
		this.name = arg0;
	}

	@Override
	public void sendMessage(String arg0) 
	{
		// Do Nothing
	}

	@Override
	public boolean a( int arg0, String arg1 )
	{
		return false;
	}

	@Override
	public ChunkCoordinates b()
	{
		// Do nothing
		return null;
	}

}
