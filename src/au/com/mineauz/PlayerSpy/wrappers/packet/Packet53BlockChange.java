package au.com.mineauz.PlayerSpy.wrappers.packet;

import au.com.mineauz.PlayerSpy.storage.StoredBlock;
import au.com.mineauz.PlayerSpy.wrappers.WrapperClass;

@WrapperClass("net.minecraft.server.*.Packet53BlockChange")
public class Packet53BlockChange extends Packet
{
	static
	{
		initialize(Packet53BlockChange.class);
		
		validateField(Packet53BlockChange.class, "a", Integer.TYPE);
		validateField(Packet53BlockChange.class, "b", Integer.TYPE);
		validateField(Packet53BlockChange.class, "c", Integer.TYPE);
		
		validateField(Packet53BlockChange.class, "material", Integer.TYPE);
		validateField(Packet53BlockChange.class, "data", Integer.TYPE);
	}
	
	Packet53BlockChange() {}
	
	public Packet53BlockChange(StoredBlock block)
	{
		super();
		instanciate();
		
		setFieldInstance("a", block.getLocation().getBlockX());
		setFieldInstance("b", block.getLocation().getBlockY());
		setFieldInstance("c", block.getLocation().getBlockZ());
		
		setFieldInstance("material", block.getTypeId());
		setFieldInstance("data", block.getData());
	}
}
