package au.com.mineauz.PlayerSpy.LogTasks;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;

public class AppendRecordsTask extends Task<Boolean> 
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
		Debug.fine("AppendRecordsTask starting");
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
		
		Debug.fine("AppendRecordsTask completed");
		return res;
	}
	@Override
	public int getTaskTargetId()
	{
		return mLog.getName().hashCode();
	}
	
	@Override
	public au.com.mineauz.PlayerSpy.LogTasks.Task.Priority getTaskPriority()
	{
		return Priority.Normal;
	}
	
}
