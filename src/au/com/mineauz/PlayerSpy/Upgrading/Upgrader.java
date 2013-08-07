package au.com.mineauz.PlayerSpy.Upgrading;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.SessionInfoRecord;
import au.com.mineauz.PlayerSpy.Records.UpdateInventoryRecord;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.globalreference.GRFileHeader;
import au.com.mineauz.PlayerSpy.globalreference.GlobalReferenceFile;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;
import au.com.mineauz.PlayerSpy.tracdata.FileHeader;
import au.com.mineauz.PlayerSpy.tracdata.SessionEntry;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;
import au.com.mineauz.PlayerSpy.tracdata.LogFileRegistry;

public class Upgrader 
{
	private static boolean mAddNeeded = false;
	private static HashSet<File> mToAdd = new HashSet<File>();
	
	public static void upgradeIndex()
	{
		mAddNeeded = false;
		
		GlobalReferenceFile index = new GlobalReferenceFile();
		
		File path = new File(SpyPlugin.getInstance().getDataFolder(), "data/reference");
		
		if(!path.exists())
		{
			LogUtil.severe("No reference exists! It will be generated");
			mAddNeeded = true;
			return;
		}
		
		boolean loaded = false;
		
		try
		{
			loaded = index.load(path);
		}
		catch(Exception e)
		{
			LogUtil.severe("reference failed to load, it will be regenerated");
			
			if(!path.delete())
			{
				LogUtil.warning("Upgrade failed on reference");
				return;
			}
			mAddNeeded = true;
		}
		
		if(path.exists() && loaded)
		{
			if(index.getVersionMajor() < GRFileHeader.currentVersion)
			{
				LogUtil.info("Upgrading reference From version " + index.getVersionMajor() + "." + index.getVersionMinor() + " to 2.0");
				index.close();
				
				if(!path.delete())
				{
					LogUtil.warning("Upgrade failed on reference");
					return;
				}
				
				// Version 1 - 2 just delete it and recreate it later
				mAddNeeded = true;
			}
			else
				index.close();
		}
		
	}
	
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
			
			if(header == null)
				continue;
			
			if(header.VersionMajor < FileHeader.currentVersion)
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
			else
				mToAdd.add(file);
		}
		
		if(mAddNeeded)
		{
			for(File file : mToAdd)
			{
				LogFile log = new LogFile();
				if(log.load(file.getAbsolutePath()))
				{
					for(SessionEntry session : log.getSessions())
						CrossReferenceIndex.addSession(log, session);
				}
			}
		}
	}
	
	/**
	 * Upgrades a log to the latest version.
	 * WARNING: This will force close the log and may take quite a while depending on the size of the log
	 */
	private static boolean upgradeLog(LogFile log)
	{
		if(log.getVersionMajor() == FileHeader.currentVersion)
			return false;
		
		LogUtil.info("Upgrading " + log.getFile().getName() + " From version " + log.getVersionMajor() + "." + log.getVersionMinor() + " to 4.0");
		
		try
		{
			// Create a new temporary file
			File logFilePath = new File(log.getFile().getAbsolutePath());
			
			File file = new File(log.getFile().getParentFile(), "upgradeTemp.tmp");
			LogFile newVersion = LogFile.create(log.getName(), file.getAbsolutePath());
	
			HashMap<Class<? extends Record>, List<Class<? extends Record>>> upgraderMap = mUpgraderMap.get(log.getVersionMajor());
			
			int index = -1;
			// Upgrade each session
			for(SessionEntry session : log.getSessions())
			{
				index++;
				RecordList old = log.loadSession(session);
				if(old == null || old.isEmpty())
					continue;
	
				LogUtil.info("Upgrading session " + session.Id + " " + (index+1) + "/" + log.getSessions().size());
				
				// Run each record through the upgrader
				if(upgraderMap != null)
				{
					RecordList newList = new RecordList();
					if(log.getVersionMajor() == 1) // Version 1 only had deep mode, so add these in
						newList.add(new SessionInfoRecord(true));
					
					for(Record oldRecord : old)
					{
						// Get rid of these
						if(oldRecord instanceof UpdateInventoryRecord)
						{
							if(((UpdateInventoryRecord)oldRecord).Slots.isEmpty())
								continue;
						}
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
					
					if(log.getVersionMajor() > 1)
					{
						String ownerTag = log.getOwnerTag(session);
						if(ownerTag != null)
							newVersion.appendRecords(old, ownerTag);
						else
							newVersion.appendRecords(old);
					}
					else
						newVersion.appendRecords(old);
				}
			}
			
			// Force close the old log
			while(log.isLoaded())
				log.close(true);
			
			// Delete the old log
			log.getFile().delete();
			
			// Force close the new log
			while(newVersion.isLoaded())
				newVersion.close(true);
			
			// Rename the new file
			newVersion.getFile().renameTo(logFilePath);
		}
		catch(Exception e)
		{
			Debug.logException(e);
		}
		
		return true;
	}
	
	@SuppressWarnings( "unused" )
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
		
		
	}

	
}
