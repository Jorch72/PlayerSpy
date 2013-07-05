package au.com.mineauz.PlayerSpy.Utilities;

import net.minecraft.server.v1_6_R1.ChatMessage;
import net.minecraft.server.v1_6_R1.ChunkCoordinates;
import net.minecraft.server.v1_6_R1.EntityHuman;
import net.minecraft.server.v1_6_R1.World;

// TODO: Make this class dynamic
public class EntityShadowPlayer extends EntityHuman
{
	public EntityShadowPlayer(World world, String name) 
	{
		super(world, name);
	}

	@Override
	public void a(String arg0) 
	{
	}

	@Override
	public void sendMessage(ChatMessage arg0) 
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
