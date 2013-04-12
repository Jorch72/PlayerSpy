package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import au.com.mineauz.PlayerSpy.wrappers.FieldWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperField;

@WrapperClass("net.minecraft.server.*.EntityLiving")
public class EntityLiving extends Entity
{
	static
	{
		initialize(EntityLiving.class);
	}
	
	@WrapperField(name="aB", type=Float.class)
	public FieldWrapper<Float> lastCamYaw;
	@WrapperField(name="aA", type=Float.class)
	public FieldWrapper<Float> camYaw;
	
	@WrapperField(name="bb", type=Float.class)
	public FieldWrapper<Float> lastCamPitch;
	@WrapperField(name="bc", type=Float.class)
	public FieldWrapper<Float> camPitch;
}
