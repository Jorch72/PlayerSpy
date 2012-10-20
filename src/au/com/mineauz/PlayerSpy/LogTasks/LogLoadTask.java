package au.com.mineauz.PlayerSpy.LogTasks;

import au.com.mineauz.PlayerSpy.LogFile;

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
		return mLog.load(mFilename);
	}
	@Override
	public int getTaskTargetId()
	{
		return -1;
	}
	
}
