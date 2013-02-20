package au.com.mineauz.PlayerSpy.wrappers.nbt;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.NBTTagFloat")
public class NBTTagFloat extends NBTBase
{
	static
	{
		initialize(NBTTagFloat.class);
		
		validateField(NBTTagFloat.class, "data", Float.TYPE);
	}
	
	@WrapperConstructor({String.class, Float.class})
	private static Constructor<?> mConstructor;
	
	public NBTTagFloat(String name, Float data)
	{
		instanciate(mConstructor, name, data);
	}
	
	public Float getData()
	{
		return getFieldInstance("data");
	}
}
