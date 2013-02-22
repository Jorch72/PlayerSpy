package au.com.mineauz.PlayerSpy.wrappers.minecraft;

import au.com.mineauz.PlayerSpy.wrappers.FieldWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperField;

@WrapperClass("net.minecraft.server.*.EntityHuman")
public class EntityHuman extends EntityLiving
{
	static
	{
		initialize(EntityHuman.class);
	}
	
	@WrapperField(name="name", type=String.class)
	public FieldWrapper<String> name;
}
