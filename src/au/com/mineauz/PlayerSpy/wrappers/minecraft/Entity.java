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
	
	@WrapperField(name="id", type=int.class)
	public FieldWrapper<Integer> id;
	
	@WrapperField(name="positionChanged", type=boolean.class)
	public FieldWrapper<Boolean> positionChanged;
	
	@WrapperField(name="velocityChanged", type=boolean.class)
	public FieldWrapper<Boolean> velocityChanged;
	
	
	@WrapperField(name="lastYaw", type=float.class)
	public FieldWrapper<Float> lastYaw;
	
	@WrapperField(name="yaw", type=float.class)
	public FieldWrapper<Float> yaw;
	
	
	@WrapperField(name="lastPitch", type=float.class)
	public FieldWrapper<Float> lastPitch;
	
	@WrapperField(name="pitch", type=float.class)
	public FieldWrapper<Float> pitch;

	@WrapperField(name="motX",type=double.class)
	public FieldWrapper<Double> motionX;
	@WrapperField(name="motY",type=double.class)
	public FieldWrapper<Double> motionY;
	@WrapperField(name="motZ",type=double.class)
	public FieldWrapper<Double> motionZ;
	
	@WrapperField(name="lastX",type=double.class)
	public FieldWrapper<Double> lastX;
	@WrapperField(name="lastY",type=double.class)
	public FieldWrapper<Double> lastY;
	@WrapperField(name="lastZ",type=double.class)
	public FieldWrapper<Double> lastZ;
	
	@WrapperField(name="locX",type=double.class)
	public FieldWrapper<Double> locationX;
	@WrapperField(name="locY",type=double.class)
	public FieldWrapper<Double> locationY;
	@WrapperField(name="locZ",type=double.class)
	public FieldWrapper<Double> locationZ;
	
	@WrapperField(name="height", type=float.class)
	public FieldWrapper<Float> height;
	
	@WrapperMethod(name="getDataWatcher", returnType=DataWatcher.class,parameterTypes={})
	private static Method mGetDataWatcher;
	
	public DataWatcher getDataWatcher()
	{
		return callMethod(mGetDataWatcher);
	}
	
	@WrapperMethod(name="setLocation", returnType=Void.class, parameterTypes={double.class, double.class, double.class, float.class, float.class})
	private static Method mSetLocation;
	
	public void setLocation(double x, double y, double z, float yaw, float pitch)
	{
		callMethod(mSetLocation, x, y, z, yaw, pitch);
	}
	
	@WrapperMethod(name="setSneaking", returnType=void.class, parameterTypes=boolean.class)
	private static Method mSetSneaking;
	
	public void setSneaking(boolean value)
	{
		callMethod(mSetSneaking, value);
	}
	
	@WrapperMethod(name="setSprinting", returnType=void.class, parameterTypes=boolean.class)
	private static Method mSetSprinting;
	
	public void setSprinting(boolean value)
	{
		callMethod(mSetSprinting, value);
	}
}
