package au.com.mineauz.PlayerSpy.wrappers.nbt;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.NBTTagInt")
public class NBTTagInt extends NBTBase
{
	static
	{
		initialize(NBTTagInt.class);
		
		validateField(NBTTagInt.class, "data", Integer.TYPE);
	}
	
	@WrapperConstructor({String.class, Integer.class})
	private static Constructor<?> mConstructor;
	
	public NBTTagInt(String name, Integer data)
	{
		instanciate(mConstructor, name, data);
	}
	protected NBTTagInt() {}
	
	public Integer getData()
	{
		return getFieldInstance("data");
	}
}
