package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.World;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.storage.InventorySlot;

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

	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof UpdateInventoryRecord))
			return false;
		
		UpdateInventoryRecord record = (UpdateInventoryRecord)obj;
		
		return Slots.equals(record.Slots);
	}
	
	@Override
	public String toString()
	{
		return "UpdateInventoryRecord { Slots: " + Slots.toString() + " }";
	}
}
