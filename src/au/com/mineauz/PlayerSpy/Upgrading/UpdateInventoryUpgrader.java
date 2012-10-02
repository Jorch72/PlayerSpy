package au.com.mineauz.PlayerSpy.Upgrading;

import java.util.ArrayList;

import au.com.mineauz.PlayerSpy.InventorySlot;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.legacy.UpdateInventoryRecord;

@SuppressWarnings("deprecation")
public class UpdateInventoryUpgrader extends RecordUpgrader 
{

	@Override
	public void upgrade(int versionMajor, int versionMinor, Record record, RecordList output) 
	{
		if(versionMajor == 1)
		{
			UpdateInventoryRecord urecord = ((UpdateInventoryRecord)record);
			// Prepare the slot
			InventorySlot slot = new InventorySlot();
			slot.Slot = urecord.getSlotId();
			slot.Item = urecord.getItem();
			
			ArrayList<InventorySlot> slots = new ArrayList<InventorySlot>();
			slots.add(slot);
			
			output.add(new au.com.mineauz.PlayerSpy.Records.UpdateInventoryRecord(slots));
		}
		else
			throw new IllegalArgumentException("Unknown version " + versionMajor);
	}

}
