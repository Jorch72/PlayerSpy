package au.com.mineauz.PlayerSpy.wrappers.nbt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.NBTCompressedStreamTools")
public class NBTCompressedStreamTools extends AutoWrapper
{
	static
	{
		initialize(NBTCompressedStreamTools.class);
	}
	
	@WrapperMethod(name="a", returnType=NBTTagCompound.class, parameterTypes=InputStream.class)
	private static Method mReadCompressed;
	
	public static NBTTagCompound readCompressed(InputStream stream) throws IOException
	{
		return callStaticMethod(mReadCompressed, stream);
	}
	
	@WrapperMethod(name="a", returnType=void.class, parameterTypes={NBTTagCompound.class, OutputStream.class})
	private static Method mWriteCompressed;
	
	public static void writeCompressed(NBTTagCompound tag, OutputStream stream) throws IOException
	{
		callStaticMethod(mWriteCompressed, tag, stream);
	}
}
