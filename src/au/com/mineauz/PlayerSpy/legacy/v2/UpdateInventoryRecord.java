package au.com.mineauz.PlayerSpy.legacy.v2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.World;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Records.RecordType;
@Deprecated
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
	
	public UpdateInventoryRecord(au.com.mineauz.PlayerSpy.legacy.UpdateInventoryRecord old)
	{
		super(RecordType.UpdateInventory);
		// Prepare the slot
		InventorySlot slot = new InventorySlot();
		slot.Slot = old.getSlotId();
		slot.Item = old.getItem();
		
		Slots = new ArrayList<InventorySlot>();
		Slots.add(slot);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeByte((byte)Slots.size());
		for(InventorySlot slot : Slots)
			slot.write(stream);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		int count = stream.readByte();
		if(count < 0 || count >= 40)
			throw new RecordFormatException("Slot count out of range");
		Slots = new ArrayList<InventorySlot>(count);
		
		for(int i = 0; i < count; i++)
			Slots.add(InventorySlot.read(stream));
	}

	@Override
	protected int getContentSize(boolean absolute) 
	{
		int size = 1;
		for(InventorySlot slot : Slots)
			size += slot.getSize();
		
		return size;
	}
	@Override
	public String getDescription()
	{
		return null;
	}

}
