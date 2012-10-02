package au.com.mineauz.PlayerSpy.Upgrading;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.Record;

public abstract class RecordUpgrader 
{
	/**
	 * Upgrades a record from a specified version.
	 * @param versionMajor The major version of the log file
	 * @param versionMinor The minor version of the log file
	 * @param record The record to upgrade
	 * @param output The list that the resultant records should be appended to
	 */
	public abstract void upgrade(int versionMajor, int versionMinor, Record record, RecordList output);
}
