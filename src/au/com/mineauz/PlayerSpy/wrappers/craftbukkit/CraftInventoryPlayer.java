package au.com.mineauz.PlayerSpy.wrappers.craftbukkit;

import java.lang.reflect.Constructor;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperConstructor;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.PlayerInventory;

@WrapperClass("org.bukkit.craftbukkit.*.inventory.CraftInventoryPlayer")
public class CraftInventoryPlayer extends AutoWrapper
{
	static
	{
		initialize(CraftInventoryPlayer.class);
	}
	
	@WrapperConstructor(PlayerInventory.class)
	private static Constructor<?> mConstructor;
	
	CraftInventoryPlayer() {}
	
	public CraftInventoryPlayer(PlayerInventory inventory)
	{
		super();
		instanciate(mConstructor, inventory);
	}
}
