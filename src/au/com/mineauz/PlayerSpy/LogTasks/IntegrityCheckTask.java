package au.com.mineauz.PlayerSpy.LogTasks;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;

import au.com.mineauz.PlayerSpy.tracdata.LogFile;
import au.com.mineauz.PlayerSpy.tracdata.LogFileRegistry;
import au.com.mineauz.PlayerSpy.tracdata.SessionEntry;

public class IntegrityCheckTask implements Task<IntegrityStats>
{
	//private boolean mCleanErrors;
	
	public IntegrityCheckTask(boolean cleanErrors)
	{
		//mCleanErrors = cleanErrors;
	}

	@Override
	public IntegrityStats call() throws Exception
	{
		File[] files = LogFileRegistry.getLogFileDirectory().listFiles(new FileFilter()
		{
			
			@Override
			public boolean accept( File file )
			{
				return file.getName().endsWith(".trackdata");
			}
		});
		
		int totalSessions = 0;
		int totalCorruptSessions = 0;
		
		ArrayList<String> corruptLogs = new ArrayList<String>();
		ArrayList<String> allLogs = new ArrayList<String>();
		HashMap<String, Integer> sessions = new HashMap<String, Integer>();
		HashMap<String, Integer> corruptSessions = new HashMap<String, Integer>();
		
		// Check each file for corrupt sessions
		for(File file : files)
		{
			LogFile log = new LogFile();
			allLogs.add(file.getName());
			
			if(!log.load(file.getAbsolutePath()))
			{
				corruptLogs.add(file.getName());
				continue;
			}
			
			sessions.put(file.getName(), 0);
			corruptSessions.put(file.getName(), 0);
			
			for(SessionEntry session : log.getSessions())
			{
				totalSessions++;
				
				if(log.loadSession(session) != null)
					sessions.put(file.getName(), sessions.get(file.getName()) + 1);
				else
				{
					corruptSessions.put(file.getName(), corruptSessions.get(file.getName()) + 1);
					totalCorruptSessions++;
				}
			}
			
			log.close(true);
		}
		
		IntegrityStats stats = new IntegrityStats();
		stats.mAllLogs = allLogs;
		stats.mCorruptLogs = corruptLogs;
		stats.mCorruptSessions = corruptSessions;
		stats.mSessions = sessions;
		stats.mTotalCorruptSessions = totalCorruptSessions;
		stats.mTotalSessions = totalSessions;
		
		return stats;
	}


	@Override
	public int getTaskTargetId()
	{
		return -1;
	}
}


