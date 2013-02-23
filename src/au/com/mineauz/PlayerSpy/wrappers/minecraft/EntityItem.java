package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.EntityItem")
public class EntityItem extends Entity
{
	static
	{
		initialize(EntityItem.class);
	}
	
	EntityItem() {}
	
	@WrapperConstructor({World.class, Double.class, Double.class, Double.class, ItemStack.class})
	private static Constructor<?> mConstructor;
	
	public EntityItem(World world, double x, double y, double z, ItemStack item)
	{
		super();
		instanciate(mConstructor, world, x, y, z, item);
	}
	
}
