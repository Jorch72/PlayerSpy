package au.com.mineauz.PlayerSpy.Upgrading;

import org.bukkit.Material;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.storage.StoredBlock;

public class BlockChangeUpgrader extends RecordUpgrader
{

	@SuppressWarnings("deprecation")
	@Override
	public void upgrade(int versionMajor, int versionMinor, Record record, RecordList output) 
	{
		au.com.mineauz.PlayerSpy.legacy.BlockChangeRecord oldRecord = (au.com.mineauz.PlayerSpy.legacy.BlockChangeRecord)record;
		BlockChangeRecord newRecord = new BlockChangeRecord();

		newRecord.setInitialBlock(new StoredBlock(oldRecord.getInitialBlock().BlockLocation, Material.getMaterial(oldRecord.getInitialBlock().BlockId), oldRecord.getInitialBlock().BlockData));
		newRecord.setFinalBlock(new StoredBlock(oldRecord.getFinalBlock().BlockLocation, Material.getMaterial(oldRecord.getFinalBlock().BlockId), oldRecord.getFinalBlock().BlockData));
		newRecord.setPlaced(oldRecord.getFinalBlock().BlockId != 0);
		
		output.add(newRecord);
	}

}
