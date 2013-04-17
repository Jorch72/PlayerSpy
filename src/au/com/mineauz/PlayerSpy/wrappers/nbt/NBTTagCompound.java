package au.com.mineauz.PlayerSpy.wrappers.nbt;

import au.com.mineauz.PlayerSpy.wrappers.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.NBTTagCompound")
public class NBTTagCompound extends NBTBase
{
	static
	{
		initialize(NBTTagCompound.class);

	}
	
	@WrapperConstructor({String.class})
	private static Constructor<?> mConstructor;
	
	public NBTTagCompound(String name)
	{
		instanciate(mConstructor, name);
	}
	protected NBTTagCompound() {}
	
	@WrapperMethod(name="c", returnType=Collection.class, parameterTypes={})
	private static Method mGetTags;
	public Collection<NBTBase> getTags()
	{
		Collection<?> rawTags = callMethod(mGetTags);
		
		Collection<NBTBase> tags = new ArrayList<NBTBase>(rawTags.size());
		for(Object rawTag : rawTags)
		{
			tags.add((NBTBase)instanciateWrapper(rawTag));
		}
		
		return tags;
	}
	
	@WrapperMethod(name="set", returnType=Void.class, parameterTypes={String.class, NBTBase.class})
	private static Method mSetMethod;
	public void set(String name, NBTBase tag)
	{
		callMethod(mSetMethod, name, tag);
	}
	
	@WrapperMethod(name="getList", returnType=NBTTagList.class, parameterTypes={String.class})
	private static Method mGetListMethod;
	public NBTTagList getList(String name)
	{
		return callMethod(mGetListMethod, name);
	}
	
	@WrapperMethod(name="getString", returnType=String.class, parameterTypes={String.class})
	private static Method mGetStringMethod;
	public String getString(String name)
	{
		return callMethod(mGetStringMethod, name);
	}
	
	@WrapperMethod(name="getInt", returnType=int.class, parameterTypes={String.class})
	private static Method mGetIntMethod;
	public int getInt(String name)
	{
		return callMethod(mGetIntMethod, name);
	}
	
	@WrapperMethod(name="getByte", returnType=byte.class, parameterTypes={String.class})
	private static Method mGetByteMethod;
	public byte getByte(String name)
	{
		return callMethod(mGetByteMethod, name);
	}
	
	@WrapperMethod(name="get", returnType=NBTBase.class, parameterTypes={String.class})
	private static Method mGetMethod;
	public NBTBase get(String name)
	{
		return callMethod(mGetMethod, name);
	}
	
	@WrapperMethod(name="hasKey", returnType=boolean.class, parameterTypes={String.class})
	private static Method mHasKeyMethod;
	public boolean hasKey(String name)
	{
		return callMethod(mHasKeyMethod, name);
	}
}
