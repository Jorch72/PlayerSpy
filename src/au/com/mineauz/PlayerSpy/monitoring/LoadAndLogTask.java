package au.com.mineauz.PlayerSpy.monitoring;

import java.util.concurrent.Callable;

import org.bukkit.OfflinePlayer;

import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.RecordList;

public class LoadAndLogTask implements Callable<Boolean>
{
	private OfflinePlayer mPlayer;
	private RecordList mRecords;
	private String mCause;
	public LoadAndLogTask(OfflinePlayer player, RecordList records, String cause)
	{
		mPlayer = player;
		mRecords = records;
		mCause = cause;
	}
	@Override
	public Boolean call() throws Exception 
	{
		LogFile log = LogFileRegistry.getLogFile(mPlayer);
		
		if(log == null)
			return false;
		
		boolean result = true;
		if(mCause != null)
		{
			if(!log.appendRecords(mRecords, mCause))
				result = false;
		}
		else
		{
			if(!log.appendRecords(mRecords))
				result = false;
		}
		
		log.close();
		if(!log.isLoaded())
			LogFileRegistry.unloadLogFile(mPlayer);
		
		return result;
	}

}
