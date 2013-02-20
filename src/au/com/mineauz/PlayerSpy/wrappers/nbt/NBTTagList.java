package au.com.mineauz.PlayerSpy.wrappers.nbt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

import au.com.mineauz.PlayerSpy.wrappers.*;

@WrapperClass("net.minecraft.server.*.NBTTagList")
public class NBTTagList extends NBTBase
{
	static
	{
		initialize(NBTTagList.class);
	}
	
	@WrapperConstructor({String.class})
	private static Constructor<?> mConstructor;
	
	public NBTTagList(String name)
	{
		instanciate(mConstructor, name);
	}
	
	@WrapperMethod(name="size",returnType=Integer.class, parameterTypes={})
	private static Method mSizeMethod;
	public int size()
	{
		return callMethod(mSizeMethod, (Object[])null);
	}
	
	@WrapperMethod(name="add",returnType=Void.class, parameterTypes={NBTBase.class})
	private static Method mAddMethod;
	public void add(NBTBase tag)
	{
		callMethod(mAddMethod, tag);
	}
	
	@WrapperMethod(name="get",returnType=NBTBase.class, parameterTypes={Integer.class})
	private static Method mGetMethod;
	public NBTBase get(int index)
	{
		return (NBTBase) instanciateWrapper(callMethod(mGetMethod, index));
	}
			
}
