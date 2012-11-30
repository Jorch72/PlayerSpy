package au.com.mineauz.PlayerSpy.LogTasks;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;

import au.com.mineauz.PlayerSpy.FileHeader;
import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;

public class LogLoadTask implements Task<Boolean>
{
	private LogFile mLog;
	private String mFilename;
	
	public LogLoadTask(LogFile log, String filename)
	{
		mLog = log;
		mFilename = filename;
	}
	
	@Override
	public Boolean call() throws Exception
	{
		if (!mLog.load(mFilename))
		{
			// We need a log, so make a backup of this one, and make a new one
			int num = 1;
			while(new File(mFilename + ".bak" + num).exists())	num++;
			
			File backup = new File(mFilename + ".bak" + num);
			try
			{
				Files.copy(new File(mFilename), backup);
				LogFile temp;
				
				// Try to get the logname
				FileHeader header = LogFile.scrapeHeader(mFilename);
				if (header != null)
				{
					CrossReferenceIndex.instance.removeLogFile(header.PlayerName);
					temp = LogFile.create(header.PlayerName, mFilename);
				}
				else
				{
					// Try to extract the playername from the filename
					String name = new File(mFilename).getName();
					if (name.contains("."))
						name = name.substring(0, name.indexOf("."));
					
					CrossReferenceIndex.instance.removeLogFile(name);
					temp = LogFile.create(name, mFilename);
				}
					
				if (temp != null)
				{
					temp.close(true);
					mLog.load(mFilename);
					// Should be all good now
					LogUtil.warning("Log Load fail: " + mFilename + ". Replacement log has been installed and old log backed up.");
				}
				else
				{
					LogUtil.severe("Log Load fail: " + mFilename + ". Replacement log also failed. IO Error");
					return false;
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
				LogUtil.severe("Cannot generate replacement log file!");
				// What do you do when your error handler fails :/
				return false;
			}
		}
		
		return true;
	}
	@Override
	public int getTaskTargetId()
	{
		return -1;
	}
	
}
