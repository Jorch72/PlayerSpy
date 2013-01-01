package au.com.mineauz.PlayerSpy.Upgrading;

import org.bukkit.Material;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.legacy.v2.InteractRecord;
import au.com.mineauz.PlayerSpy.storage.StoredBlock;

public class InteractRecordUpgrader extends RecordUpgrader
{

	@SuppressWarnings("deprecation")
	@Override
	public void upgrade(int versionMajor, int versionMinor, Record record, RecordList output) 
	{
		au.com.mineauz.PlayerSpy.legacy.InteractRecord oldRecord = (au.com.mineauz.PlayerSpy.legacy.InteractRecord)record;
		
		StoredBlock block = null;
		if(oldRecord.getBlock() != null)
		{
			block = new StoredBlock(oldRecord.getBlock().BlockLocation, Material.getMaterial(oldRecord.getBlock().BlockId), oldRecord.getBlock().BlockData);
		}

		InteractRecord newRecord = new InteractRecord(oldRecord.getAction(), block, oldRecord.getItem(), oldRecord.getEntity());
		output.add(newRecord);
	}

}
