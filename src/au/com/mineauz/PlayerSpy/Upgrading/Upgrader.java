package au.com.mineauz.PlayerSpy.Upgrading;

import java.io.File;
import java.util.HashMap;

import au.com.mineauz.PlayerSpy.FileHeader;
import au.com.mineauz.PlayerSpy.IndexEntry;
import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Records.SessionInfoRecord;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;

public class Upgrader 
{
	public static void run()
	{
		File dir = LogFileRegistry.getLogFileDirectory();
		
		for(File file : dir.listFiles())
		{
			if(!file.isFile())
				continue;
			
			if(!file.getName().endsWith(LogFileRegistry.cFileExt))
				continue;
			
			// Check versions
			FileHeader header = LogFile.scrapeHeader(file.getAbsolutePath());
			if(header.VersionMajor != 2)
			{
				LogFile log = new LogFile();
				if(log.load(file.getAbsolutePath()))
				{
					if(!upgradeLog(log))
						LogUtil.warning("Upgrade failed on " + file.getName());
				}
				else
					LogUtil.warning("Upgrade failed on " + file.getName());
			}
		}
	}
	
	/**
	 * Upgrades a log to the latest version.
	 * WARNING: This will force close the log and may take quite a while depending on the size of the log
	 */
	private static boolean upgradeLog(LogFile log)
	{
		if(log.getVersionMajor() == 2)
			return false;
		
		LogUtil.info("Upgrading " + log.getFile().getName() + " From version " + log.getVersionMajor() + "." + log.getVersionMinor() + " to 2.0");
		
		// Create a new temporary file
		File logFilePath = new File(log.getFile().getAbsolutePath());
		
		File file = new File(log.getFile().getParentFile(), "upgradeTemp.tmp");
		LogFile newVersion = LogFile.create(log.getName(), file.getAbsolutePath());

		HashMap<RecordType, RecordUpgrader> upgraderMap = mUpgraderMap.get(log.getVersionMajor());
		
		int index = -1;
		// Upgrade each session
		for(IndexEntry session : log.getSessions())
		{
			index++;
			RecordList old = log.loadSession(session);
			if(old == null)
				continue;
		
			// Run each record through the upgrader
			if(upgraderMap != null)
			{
				RecordList newList = new RecordList();
				if(log.getVersionMajor() == 1) // Version 1 only had deep mode, so add these in
					newList.add(new SessionInfoRecord(true));
				
				for(Record oldRecord : old)
				{
					if(upgraderMap.containsKey(oldRecord.getType()))
						// Use the upgrader
						upgraderMap.get(oldRecord.getType()).upgrade(log.getVersionMajor(), log.getVersionMinor(), oldRecord, newList);
					else
						// No upgrader for it
						newList.add(oldRecord);
				}
				
				if(index == log.getSessions().size()-1 && log.getVersionMajor() == 1)
					// Version 1 only had deep mode, so add these in
					old.add(new SessionInfoRecord(false));
				
				newVersion.appendRecords(newList);
			}
			else
			{
				if(log.getVersionMajor() == 1) // Version 1 only had deep mode, so add these in
				{
					old.add(0, new SessionInfoRecord(true));
					if(index == log.getSessions().size()-1)
						old.add(new SessionInfoRecord(false));
				}
				
				
				newVersion.appendRecords(old);
			}
		}
		
		// Force close the old log
		while(log.isLoaded())
			log.close(true);
		
		// Rename the old log
		File backupDest = new File(log.getFile().getParentFile(), log.getFile().getName() + ".bak");
		if(backupDest.exists())
			backupDest.delete();
		
		log.getFile().renameTo(backupDest);
		
		// Force close the new log
		while(newVersion.isLoaded())
			newVersion.close(true);
		
		// Rename the new file
		newVersion.getFile().renameTo(logFilePath);
		
		return true;
	}
	
	private static void registerUpgrader(int version, RecordType type, RecordUpgrader upgrader)
	{
		HashMap<RecordType, RecordUpgrader> map = null;
		map = mUpgraderMap.get(version);
		
		if(map == null)
		{
			map = new HashMap<RecordType, RecordUpgrader>();
			mUpgraderMap.put(version, map);
		}
		
		map.put(type,upgrader);
	}
	private static HashMap<Integer, HashMap<RecordType, RecordUpgrader>> mUpgraderMap;
	
	static
	{
		mUpgraderMap = new HashMap<Integer, HashMap<RecordType,RecordUpgrader>>();
		
		// ======= Version 1 =======
		registerUpgrader(1, RecordType.UpdateInventory, new UpdateInventoryUpgrader());
		registerUpgrader(1, RecordType.BlockChange, new BlockChangeUpgrader());
		registerUpgrader(1, RecordType.Interact, new InteractRecordUpgrader());
	}
}
