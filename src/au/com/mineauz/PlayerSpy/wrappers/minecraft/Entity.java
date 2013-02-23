package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.FieldWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperField;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;

@WrapperClass("net.minecraft.server.*.Entity")
public class Entity extends AutoWrapper
{
	static
	{
		initialize(Entity.class);
	}
	
	@WrapperField(name="world", type=World.class)
	public FieldWrapper<World> world;
	
	@WrapperField(name="id", type=Integer.class)
	public FieldWrapper<Integer> id;
	
	@WrapperField(name="positionChanged", type=Boolean.class)
	public FieldWrapper<Boolean> positionChanged;
	
	@WrapperField(name="velocityChanged", type=Boolean.class)
	public FieldWrapper<Boolean> velocityChanged;
	
	
	@WrapperField(name="lastYaw", type=Float.class)
	public FieldWrapper<Float> lastYaw;
	
	@WrapperField(name="yaw", type=Float.class)
	public FieldWrapper<Float> yaw;
	
	
	@WrapperField(name="lastPitch", type=Float.class)
	public FieldWrapper<Float> lastPitch;
	
	@WrapperField(name="pitch", type=Float.class)
	public FieldWrapper<Float> pitch;

	@WrapperField(name="motX",type=Double.class)
	public FieldWrapper<Double> motionX;
	@WrapperField(name="motY",type=Double.class)
	public FieldWrapper<Double> motionY;
	@WrapperField(name="motZ",type=Double.class)
	public FieldWrapper<Double> motionZ;
	
	@WrapperField(name="lastX",type=Double.class)
	public FieldWrapper<Double> lastX;
	@WrapperField(name="lastY",type=Double.class)
	public FieldWrapper<Double> lastY;
	@WrapperField(name="lastZ",type=Double.class)
	public FieldWrapper<Double> lastZ;
	
	@WrapperField(name="locX",type=Double.class)
	public FieldWrapper<Double> locationX;
	@WrapperField(name="locY",type=Double.class)
	public FieldWrapper<Double> locationY;
	@WrapperField(name="locZ",type=Double.class)
	public FieldWrapper<Double> locationZ;
	
	@WrapperField(name="height", type=Float.class)
	public FieldWrapper<Float> height;
	
	@WrapperMethod(name="getDataWatcher", returnType=DataWatcher.class,parameterTypes={})
	private static Method mGetDataWatcher;
	
	public DataWatcher getDataWatcher()
	{
		return callMethod(mGetDataWatcher);
	}
	
	@WrapperMethod(name="setLocation", returnType=Void.class, parameterTypes={Double.class, Double.class, Double.class, Float.class, Float.class})
	private static Method mSetLocation;
	
	public void setLocation(double x, double y, double z, float yaw, float pitch)
	{
		callMethod(mSetLocation, x, y, z, yaw, pitch);
	}
	
	@WrapperMethod(name="setSneaking", returnType=Void.class, parameterTypes=Boolean.class)
	private static Method mSetSneaking;
	
	public void setSneaking(boolean value)
	{
		callMethod(mSetSneaking, value);
	}
	
	@WrapperMethod(name="setSprinting", returnType=Void.class, parameterTypes=Boolean.class)
	private static Method mSetSprinting;
	
	public void setSprinting(boolean value)
	{
		callMethod(mSetSprinting, value);
	}
}
