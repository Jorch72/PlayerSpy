package au.com.mineauz.PlayerSpy.wrappers.nbt;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.NBTTagShort")
public class NBTTagShort extends NBTBase
{
	static
	{
		initialize(NBTTagShort.class);
		
		validateField(NBTTagShort.class, "data", Short.TYPE);
	}
	
	@WrapperConstructor({String.class, Short.class})
	private static Constructor<?> mConstructor;
	
	public NBTTagShort(String name, Short data)
	{
		instanciate(mConstructor, name, data);
	}
	
	public Short getData()
	{
		return getFieldInstance("data");
	}
}
