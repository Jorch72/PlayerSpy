package au.com.mineauz.PlayerSpy.wrappers.craftbukkit;

import java.lang.reflect.Method;

import org.bukkit.inventory.InventoryView;

import au.com.mineauz.PlayerSpy.wrappers.AutoWrapper;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;
import au.com.mineauz.PlayerSpy.wrappers.WrapperMethod;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.Container;

@WrapperClass("org.bukkit.craftbukkit.*.inventory.CraftInventoryView")
public class CraftInventoryView extends AutoWrapper
{
	static
	{
		initialize(CraftInventoryView.class);
	}
	
	CraftInventoryView() {}
	
	public static CraftInventoryView castFrom(InventoryView view)
	{
		CraftInventoryView wrapper = new CraftInventoryView();
		wrapper.mInstance = getWrappedClass(CraftInventoryView.class).cast(view);
		
		return wrapper;
	}

	@WrapperMethod(name="getHandle",returnType=Container.class,parameterTypes={})
	private static Method mGetHandle;
	
	public Container getHandle()
	{
		return callMethod(mGetHandle);
	}
}
