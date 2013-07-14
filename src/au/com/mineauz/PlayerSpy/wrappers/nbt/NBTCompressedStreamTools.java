package au.com.mineauz.PlayerSpy.wrappers.nbt;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
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
	
	@WrapperMethod(name="a", returnType=NBTTagCompound.class, parameterTypes=DataInput.class)
	private static Method mReadInput;
	
	public static NBTTagCompound read(InputStream stream) throws IOException
	{
		// NOTE: It turns out that the wrapped version closes the stream, this is not what I want, so I am replicating the functionality
		DataInputStream dInput = new DataInputStream(new BufferedInputStream(stream));

        return callStaticMethod(mReadInput, dInput);
	}
	
	@WrapperMethod(name="a", returnType=void.class, parameterTypes={NBTTagCompound.class, DataOutput.class})
	private static Method mWriteOutput;
	
	public static void write(NBTTagCompound tag, OutputStream stream) throws IOException
	{
		// NOTE: It turns out that the wrapped version closes the stream, this is not what I want, so I am replicating the functionality
		DataOutputStream dOutput = new DataOutputStream(stream);

        callStaticMethod(mWriteOutput, tag, dOutput);
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
