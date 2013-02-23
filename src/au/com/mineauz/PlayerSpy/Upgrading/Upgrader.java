package au.com.mineauz.PlayerSpy.Upgrading;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import au.com.mineauz.PlayerSpy.FileHeader;
import au.com.mineauz.PlayerSpy.IndexEntry;
import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.SessionInfoRecord;
import au.com.mineauz.PlayerSpy.legacy.InteractRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.DropItemRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.InventoryRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.InventoryTransactionRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.ItemFrameChangeRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.ItemPickupRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.PaintingChangeRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.RightClickActionRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.UpdateInventoryRecord;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;

@SuppressWarnings( "deprecation" )
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
			
			if(header != null && header.VersionMajor != 3)
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
		if(log.getVersionMajor() == 3)
			return false;
		
		LogUtil.info("Upgrading " + log.getFile().getName() + " From version " + log.getVersionMajor() + "." + log.getVersionMinor() + " to 3.0");
		
		// Create a new temporary file
		File logFilePath = new File(log.getFile().getAbsolutePath());
		
		File file = new File(log.getFile().getParentFile(), "upgradeTemp.tmp");
		LogFile newVersion = LogFile.create(log.getName(), file.getAbsolutePath());

		HashMap<Class<? extends Record>, List<Class<? extends Record>>> upgraderMap = mUpgraderMap.get(log.getVersionMajor());
		
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
					if(upgraderMap.containsKey(oldRecord.getClass()))
					{
						// Use the upgrader
						for(Class<? extends Record> clazz : upgraderMap.get(oldRecord.getClass()))
						{
							try
							{
								Constructor<? extends Record> con = clazz.getConstructor(oldRecord.getClass());
								newList.add(con.newInstance(oldRecord));
							}
							catch ( Exception e )
							{
								e.printStackTrace();
							}
						}
					}
					else
						// No upgrader for it
						newList.add(oldRecord);
				}
				
				if(index == log.getSessions().size()-1 && log.getVersionMajor() == 1)
					// Version 1 only had deep mode, so add these in
					old.add(new SessionInfoRecord(false));
				
				if(log.getVersionMajor() > 1)
				{
					String ownerTag = log.getOwnerTag(session);
					if(ownerTag != null)
						newVersion.appendRecords(newList, ownerTag);
					else
						newVersion.appendRecords(newList);
				}
				else
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
	
	@SafeVarargs
	private static void registerUpgradeMapping(int fromVersion, Class<? extends Record> oldType, Class<? extends Record>... newTypes)
	{
		// the new types must have a constructor that takes an instance of the old type
		for(Class<? extends Record> clazz : newTypes)
		{
			try
			{
				clazz.getConstructor(oldType);
			}
			catch(NoSuchMethodException e)
			{
				throw new IllegalArgumentException("Cannot use " + clazz.getName() + " as an upgrade destination. It needs to take an instance to " + oldType.getName() + " as the only argument in a constructor.");
			}
		}
		
		HashMap<Class<? extends Record>, List<Class<? extends Record>>> map = null;
		map = mUpgraderMap.get(fromVersion);
		
		if(map == null)
		{
			map = new HashMap<Class<? extends Record>, List<Class<? extends Record>>>();
			mUpgraderMap.put(fromVersion, map);
		}
		
		map.put(oldType,Arrays.asList(newTypes));
	}
	private static HashMap<Integer, HashMap<Class<? extends Record>, List<Class<? extends Record>>>> mUpgraderMap;
	
	static
	{
		mUpgraderMap = new HashMap<Integer, HashMap<Class<? extends Record>,List<Class<? extends Record>>>>();
		
		// ======= Version 1 =======
		registerUpgradeMapping(1, au.com.mineauz.PlayerSpy.legacy.UpdateInventoryRecord.class, UpdateInventoryRecord.class);
		registerUpgradeMapping(1, au.com.mineauz.PlayerSpy.legacy.BlockChangeRecord.class, BlockChangeRecord.class);
		registerUpgradeMapping(1, InteractRecord.class, au.com.mineauz.PlayerSpy.legacy.v2.InteractRecord.class);
		
		// ======= Version 2 =======
		registerUpgradeMapping(2, DropItemRecord.class, au.com.mineauz.PlayerSpy.Records.DropItemRecord.class);
		registerUpgradeMapping(2, au.com.mineauz.PlayerSpy.legacy.v2.InteractRecord.class, au.com.mineauz.PlayerSpy.Records.InteractRecord.class);
		registerUpgradeMapping(2, InventoryRecord.class, au.com.mineauz.PlayerSpy.Records.InventoryRecord.class);
		registerUpgradeMapping(2, InventoryTransactionRecord.class, au.com.mineauz.PlayerSpy.Records.InventoryTransactionRecord.class);
		registerUpgradeMapping(2, ItemPickupRecord.class, au.com.mineauz.PlayerSpy.Records.ItemPickupRecord.class);
		registerUpgradeMapping(2, RightClickActionRecord.class, au.com.mineauz.PlayerSpy.Records.RightClickActionRecord.class);
		registerUpgradeMapping(2, ItemFrameChangeRecord.class, au.com.mineauz.PlayerSpy.Records.ItemFrameChangeRecord.class);
		registerUpgradeMapping(2, UpdateInventoryRecord.class, au.com.mineauz.PlayerSpy.Records.UpdateInventoryRecord.class);
		registerUpgradeMapping(2, BlockChangeRecord.class, au.com.mineauz.PlayerSpy.Records.BlockChangeRecord.class);
		registerUpgradeMapping(2, PaintingChangeRecord.class, au.com.mineauz.PlayerSpy.Records.PaintingChangeRecord.class);
	}
}
