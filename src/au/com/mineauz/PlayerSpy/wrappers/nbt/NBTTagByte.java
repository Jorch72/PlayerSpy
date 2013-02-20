package au.com.mineauz.PlayerSpy.wrappers.nbt;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.NBTTagByte")
public class NBTTagByte extends NBTBase
{
	static
	{
		initialize(NBTTagByte.class);
		
		validateField(NBTTagByte.class, "data", Byte.TYPE);
	}
	
	@WrapperConstructor({String.class, Byte.class})
	private static Constructor<?> mConstructor;
	
	public NBTTagByte(String name, Byte data)
	{
		instanciate(mConstructor, name, data);
	}
	
	public Byte getData()
	{
		return getFieldInstance("data");
	}
}
