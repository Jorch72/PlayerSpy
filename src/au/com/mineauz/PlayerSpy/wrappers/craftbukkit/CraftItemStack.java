package au.com.mineauz.PlayerSpy.wrappers.craftbukkit;

import java.lang.reflect.Method;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.ItemStack;

@WrapperClass("org.bukkit.craftbukkit.*.inventory.CraftItemStack")
public class CraftItemStack extends AutoWrapper
{
	static
	{
		initialize(CraftItemStack.class);
	}
	
	@WrapperMethod(name="asNMSCopy", returnType=ItemStack.class, parameterTypes=org.bukkit.inventory.ItemStack.class)
	private static Method mAsNMSCopy;
	
	public static ItemStack asNMSCopy(org.bukkit.inventory.ItemStack item)
	{
		return callStaticMethod(mAsNMSCopy, item);
	}
	
	@WrapperMethod(name="asBukkitCopy", returnType=org.bukkit.inventory.ItemStack.class, parameterTypes=ItemStack.class)
	private static Method mAsBukkitCopy;
	public static org.bukkit.inventory.ItemStack asBukkitCopy(ItemStack item)
	{
		return callStaticMethod(mAsBukkitCopy, item);
	}
}
