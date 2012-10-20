package au.com.mineauz.PlayerSpy.LogTasks;

import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.RecordList;

public class AppendRecordsTask implements Task<Boolean> 
{
	private final RecordList mRecords;
	private final LogFile mLog;
	private final String mOwner;
	public AppendRecordsTask(LogFile logFile, RecordList records)
	{
		mRecords = records;
		mLog = logFile;
		mOwner = null;
	}
	public AppendRecordsTask(LogFile logFile, RecordList records, String owner)
	{
		mRecords = records;
		mLog = logFile;
		mOwner = owner;
	}

	@Override
	public Boolean call() throws Exception 
	{
		LogUtil.finer("AppendRecordsTask starting");
		boolean res;
		try
		{
			if(mOwner == null)
				res = mLog.appendRecords(mRecords);
			else
				res = mLog.appendRecords(mRecords, mOwner);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw e;
		}
		
		LogUtil.finer("AppendRecordsTask completed");
		return res;
	}
	@Override
	public int getTaskTargetId()
	{
		return mLog.getName().hashCode();
	}
	
	
	
}
