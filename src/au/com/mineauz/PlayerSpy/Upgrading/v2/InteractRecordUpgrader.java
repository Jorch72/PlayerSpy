package au.com.mineauz.PlayerSpy.Upgrading.v2;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.InteractRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Upgrading.RecordUpgrader;

public class InteractRecordUpgrader extends RecordUpgrader
{

	@SuppressWarnings( "deprecation" )
	@Override
	public void upgrade( int versionMajor, int versionMinor, Record record, RecordList output )
	{
		au.com.mineauz.PlayerSpy.legacy.v2.InteractRecord oldRecord = (au.com.mineauz.PlayerSpy.legacy.v2.InteractRecord)record;
		
		InteractRecord newRecord = new InteractRecord(oldRecord.getAction(), oldRecord.getBlock(), oldRecord.getItem(), oldRecord.getEntity());
		output.add(newRecord);
	}

}
