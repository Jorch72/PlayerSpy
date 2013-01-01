package au.com.mineauz.PlayerSpy.Upgrading.v2;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.DropItemRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Upgrading.RecordUpgrader;;

public class DropItemRecordUpgrader extends RecordUpgrader
{

	@SuppressWarnings( "deprecation" )
	@Override
	public void upgrade( int versionMajor, int versionMinor, Record record, RecordList output )
	{
		DropItemRecord newRecord = new DropItemRecord(((au.com.mineauz.PlayerSpy.legacy.v2.DropItemRecord)record).getItem());
		output.add(newRecord);
	}
	
}
