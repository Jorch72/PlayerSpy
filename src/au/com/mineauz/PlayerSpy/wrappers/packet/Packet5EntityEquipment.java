package au.com.mineauz.PlayerSpy.wrappers.packet;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.ItemStack;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;

@WrapperClass("net.minecraft.server.*.Packet5EntityEquipment")
public class Packet5EntityEquipment extends Packet
{
	static
	{
		initialize(Packet5EntityEquipment.class);
	}
	
	@WrapperConstructor({Integer.class, Integer.class, ItemStack.class})
	private static Constructor<?> mConstructor;
	
	public Packet5EntityEquipment(int entityId, int slot, ItemStack item)
	{
		instanciate(mConstructor, entityId, slot, item);
	}
}
