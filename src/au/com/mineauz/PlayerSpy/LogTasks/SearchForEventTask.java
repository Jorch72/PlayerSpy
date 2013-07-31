package au.com.mineauz.PlayerSpy.LogTasks;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;

public class SearchForEventTask extends Task<Long>
{
	private final RecordType mRecordType;
	private final long mSearchStartDate;
	private final boolean mForward;
	private final LogFile mLogFile;
	
	public SearchForEventTask(LogFile logFile, RecordType recordType, long searchStartDate, boolean forward)
	{
		mLogFile = logFile;
		mRecordType = recordType;
		mSearchStartDate = searchStartDate;
		mForward = forward;
	}

	@Override
	public Long call() throws Exception 
	{
		// Load up the initial set
		RecordList temp;
		long date = mSearchStartDate;
		
		if(date == 0)
			date = (mForward ? mLogFile.getStartDate() + 1 : mLogFile.getEndDate() - 1);
		
		temp = mLogFile.loadRecordChunks(date, date);
		int startIndex = (mForward ? temp.getNextRecordAfter(date) : temp.getLastRecordBefore(date));
		
		while(temp.size() > 0)
		{
			if(mForward)
			{
				for(int i = startIndex; i < temp.size(); i++)
				{
					if(temp.get(i).getType() == mRecordType)
						return temp.get(i).getTimestamp();
				}
			}
			else
			{
				for(int i = startIndex; i >= 0; i--)
				{
					if(temp.get(i).getType() == mRecordType)
						return temp.get(i).getTimestamp();
				}
			}
			
			if(Thread.interrupted())
				return 0L;
			
			// Load the next chunk
			if(mForward)
				date = mLogFile.getNextAvailableDateAfter(temp.getEndTimestamp());
			else
				date = mLogFile.getNextAvailableDateBefore(temp.getStartTimestamp());
			
			temp = mLogFile.loadRecordChunks(date, date);
			
			// position the cursor
			if(mForward)
				startIndex = 0;
			else
				startIndex = temp.size()-1;
		}
		
		// No record was found
		return 0L;
	}

	@Override
	public int getTaskTargetId()
	{
		return mLogFile.getName().hashCode();
	}
	
	@Override
	public au.com.mineauz.PlayerSpy.LogTasks.Task.Priority getTaskPriority()
	{
		return Priority.High;
	}
}
