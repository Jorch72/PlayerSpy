package au.com.mineauz.PlayerSpy.LogTasks;

import java.util.concurrent.Callable;

import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;


public class PurgeTask  implements Callable<Boolean>
{
	private final LogFile mLogFile;
	private final long mFromDate;
	private final long mToDate;
	public PurgeTask(LogFile logFile, long from, long to)
	{
		mLogFile = logFile;
		mFromDate = from;
		mToDate = to;
	}
	@Override
	public Boolean call() throws Exception 
	{
		LogUtil.finer("Starting PurgeTask");
		return mLogFile.purgeRecords(mFromDate, mToDate);
	}
	


}
