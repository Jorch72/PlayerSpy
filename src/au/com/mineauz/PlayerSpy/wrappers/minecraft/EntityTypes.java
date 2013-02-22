package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.EntityTypes")
public class EntityTypes extends AutoWrapper
{
	static
	{
		initialize(EntityTypes.class);
	}
	
	@WrapperMethod(name="a", returnType=Entity.class, parameterTypes={Integer.class, World.class})
	private static Method mCreateEntityByID;
	
	public static Entity createEntityByID(int id, World world)
	{
		return callStaticMethod(mCreateEntityByID, id, world);
	}
}
