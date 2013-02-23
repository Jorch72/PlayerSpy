package au.com.mineauz.PlayerSpy.wrappers.nbt;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.NBTTagLong")
public class NBTTagLong extends NBTBase
{
	static
	{
		initialize(NBTTagLong.class);
		
		validateField(NBTTagLong.class, "data", Long.TYPE);
	}
	
	@WrapperConstructor({String.class, Long.class})
	private static Constructor<?> mConstructor;
	
	public NBTTagLong(String name, Long data)
	{
		instanciate(mConstructor, name, data);
	}
	
	protected NBTTagLong() {}
	
	public Long getData()
	{
		return getFieldInstance("data");
	}
}
