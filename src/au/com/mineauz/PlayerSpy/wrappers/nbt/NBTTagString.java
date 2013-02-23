package au.com.mineauz.PlayerSpy.wrappers.nbt;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.NBTTagString")
public class NBTTagString extends NBTBase
{
	static
	{
		initialize(NBTTagString.class);
		
		validateField(NBTTagString.class, "data", String.class);
	}
	
	@WrapperConstructor({String.class, String.class})
	private static Constructor<?> mConstructor;
	
	public NBTTagString(String name, String data)
	{
		instanciate(mConstructor, name, data);
	}
	
	protected NBTTagString() {}
	
	public String getData()
	{
		return getFieldInstance("data");
	}
}
