package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("au.com.mineauz.PlayerSpy.Utilities.EntityShadowPlayer")
public class EntityShadowPlayer extends EntityHuman
{
	static
	{
		initialize(EntityShadowPlayer.class);
	}
	
	public EntityShadowPlayer() {}
	
	@WrapperConstructor({World.class, String.class})
	private static Constructor<?> mConstructor;
	
	public EntityShadowPlayer(World world, String name)
	{
		super();
		instanciate(mConstructor, world, name);
	}
	
}
