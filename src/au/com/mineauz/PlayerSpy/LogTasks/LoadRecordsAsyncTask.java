package au.com.mineauz.PlayerSpy.LogTasks;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;

public class LoadRecordsAsyncTask extends Task<RecordList>
{
	private final LogFile mLogFile;
	private final long mStartDate;
	private final long mEndDate;
	private final boolean mLoadChunks;
	private final String mOwner;
	
	public LoadRecordsAsyncTask(LogFile logFile, long startDate, long endDate, boolean chunks)
	{
		mLogFile = logFile;
		mStartDate = startDate;
		mEndDate = endDate;
		mLoadChunks = chunks;
		mOwner = null;
	}
	public LoadRecordsAsyncTask(LogFile logFile, long startDate, long endDate, boolean chunks, String owner)
	{
		mLogFile = logFile;
		mStartDate = startDate;
		mEndDate = endDate;
		mLoadChunks = chunks;
		mOwner = owner;
	}
	
	@Override
	public RecordList call() throws Exception 
	{
		Debug.fine("LoadRecordsAsyncTask starting");
		RecordList results = null;

		if(mOwner != null)
		{
			if(mLoadChunks)
				results = mLogFile.loadRecordChunks(mStartDate, mEndDate, mOwner);
			else
				results = mLogFile.loadRecords(mStartDate, mEndDate, mOwner);
		}
		else
		{
			if(mLoadChunks)
				results = mLogFile.loadRecordChunks(mStartDate, mEndDate);
			else
				results = mLogFile.loadRecords(mStartDate, mEndDate);
		}
		Debug.finer("LoadRecordsAsyncTask completed");
		return results;
	}
	@Override
	public int getTaskTargetId()
	{
		return mLogFile.getName().hashCode();
	}

	@Override
	public au.com.mineauz.PlayerSpy.LogTasks.Task.Priority getTaskPriority()
	{
		return Priority.Normal;
	}
}
