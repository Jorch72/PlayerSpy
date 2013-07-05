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
	
	@WrapperField(name="aQ", type=Float.class)
	public FieldWrapper<Float> lastCamYaw;
	@WrapperField(name="aP", type=Float.class)
	public FieldWrapper<Float> camYaw;
	
	@WrapperField(name="aJ", type=Float.class)
	public FieldWrapper<Float> lastCamPitch;
	@WrapperField(name="aK", type=Float.class)
	public FieldWrapper<Float> camPitch;
}
