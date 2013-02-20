package au.com.mineauz.PlayerSpy.wrappers.nbt;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.NBTTagDouble")
public class NBTTagDouble extends NBTBase
{
	static
	{
		initialize(NBTTagDouble.class);
		
		validateField(NBTTagDouble.class, "data", Double.TYPE);
	}
	
	@WrapperConstructor({String.class, Double.class})
	private static Constructor<?> mConstructor;
	
	public NBTTagDouble(String name, Double data)
	{
		instanciate(mConstructor, name, data);
	}
	
	public Double getData()
	{
		return getFieldInstance("data");
	}
}
