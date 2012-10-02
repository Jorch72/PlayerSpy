package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.World;

import au.com.mineauz.PlayerSpy.InventorySlot;

public class UpdateInventoryRecord extends Record
{
	public ArrayList<InventorySlot> Slots;
	public UpdateInventoryRecord() 
	{
		super(RecordType.UpdateInventory);
		Slots = new ArrayList<InventorySlot>();
	}
	public UpdateInventoryRecord(ArrayList<InventorySlot> slots)
	{
		super(RecordType.UpdateInventory);
		Slots = slots;
	}

	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		stream.writeByte((byte)Slots.size());
		for(InventorySlot slot : Slots)
			slot.write(stream);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		int count = stream.readByte();
		Slots = new ArrayList<InventorySlot>(count);
		
		for(int i = 0; i < count; i++)
			Slots.add(InventorySlot.read(stream));
	}

	@Override
	protected int getContentSize() 
	{
		int size = 1;
		for(InventorySlot slot : Slots)
			size += slot.getSize();
		
		return size;
	}

}
