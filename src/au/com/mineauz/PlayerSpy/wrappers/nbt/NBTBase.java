package au.com.mineauz.PlayerSpy.wrappers.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.NBTBase")
public abstract class NBTBase extends AutoWrapper
{
	static
	{
		initialize(NBTBase.class);
	}
	
	NBTBase() {}
	
	@WrapperMethod(name="getName", returnType=String.class, parameterTypes={})
	private static Method mGetName;
	public String getName()
	{
		return callMethod(mGetName);
	}
	
	@WrapperMethod(name="setName", returnType=NBTBase.class, parameterTypes={String.class})
	private static Method mSetName;
	public NBTBase setName(String name)
	{
		return callMethod(mSetName, name);
	}
	
	@WrapperMethod(name="a", returnType=Void.class, parameterTypes={NBTBase.class, DataOutput.class})
	private static Method mWriteNamedTag;
	
	public static void writeNamedTag(NBTBase tag, DataOutput output)
	{
		callStaticMethod(mWriteNamedTag, tag, output);
	}
	
	@WrapperMethod(name="b", returnType=NBTBase.class, parameterTypes=DataInput.class)
	private static Method mReadNamedTag;
	
	public static NBTBase readNamedTag(DataInput input)
	{
		return callStaticMethod(mReadNamedTag, input);
	}
	
}
