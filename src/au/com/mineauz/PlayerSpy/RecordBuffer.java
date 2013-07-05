package au.com.mineauz.PlayerSpy;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import au.com.mineauz.PlayerSpy.tracdata.LogFile;

public class RecordBuffer 
{
	private enum RequestType
	{
		ForwardShift,
		BackwardShift,
		ForwardSeek,
		BackwardSeek
	}
	
	private RecordList mBuffer;
	private final LogFile mLogFile;
	private int mRelativeOffset;
	private Future<RecordList> mRequest;
	private RequestType mRequestType;
	
	private long mRequestDate;
	
	private int mBufferChunkBreak;
	
	/**
	 * The threshold in ms that when the time left until the end of the buffer is lower than the threshold, more data should be requested  
	 */
	public static long sBufferUpdateThreashold = 10000;
	
	public RecordBuffer(LogFile logFile)
	{
		mLogFile = logFile;
		mLogFile.addReference();
		
		mRelativeOffset = 0;
		mBuffer = new RecordList();
		
		mRequest = null;
		mRequestDate = 0L;
		
		mBufferChunkBreak = 0;
	}
	
	/**
	 * @return Gets the current buffer
	 */
	public RecordList currentBuffer()
	{
		return mBuffer;
	}
	
	/**
	 * @return the index of the first entry in the buffer, relative to when the buffer was last seeked
	 */
	public int relativeOffset()
	{
		return mRelativeOffset;
	}
	/**
	 * @return True when the buffer is loading data currently.
	 */
	public boolean isLoading()
	{
		return mRequest != null && !mRequest.isDone();
	}
	
	/**
	 * Queries to see if more data should be requested based on the current read position within the buffer
	 * @param bufferReadPos the read position in the buffer where 0 is the start of the buffer
	 * @param forward True if the read direction is forward
	 * @return True if more data should be loaded
	 */
	public boolean shouldRequestMore(int bufferReadPos, boolean forward)
	{
		if(forward)
		{
			// Check if the request could be made
			boolean possible = false;
			if(bufferReadPos >= mBuffer.size())
				possible = true;
			else
			{
				long dateDif = mBuffer.getEndTimestamp() - mBuffer.get(bufferReadPos).getTimestamp();
				
				if(bufferReadPos > mBufferChunkBreak && dateDif < sBufferUpdateThreashold)
					possible = true;
			}
			
			if(possible)
			{
				// If there is an active request
				if(mRequest != null && !mRequest.isDone())
				{
					if(mRequestDate > mBuffer.getEndTimestamp() && mRequestDate < mBuffer.getEndTimestamp() + 1000)
						// It has already been requested
						return false;
				}
				
				return true;
			}
			return false;
		}
		else
		{
			// Check if the request could be made
			boolean possible = false;
			if(bufferReadPos < 0)
				possible = true;
			else
			{
				long dateDif = mBuffer.get(bufferReadPos).getTimestamp() - mBuffer.getStartTimestamp();
				
				if(bufferReadPos < mBufferChunkBreak && dateDif < sBufferUpdateThreashold)
					possible = true;
			}
			
			if(possible)
			{
				// If there is an active request
				if(mRequest != null && !mRequest.isDone())
				{
					if(mRequestDate < mBuffer.getStartTimestamp() && mRequestDate > mBuffer.getStartTimestamp() - 1000)
						// It has already been requested
						return false;
				}
				
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Requests that the buffer be shifted to the next chunk of records
	 * @param forward True to shift the buffer forward
	 * @return True if the request was successful. False if it could not be shifted either because no date had been specified beforehand using seekBuffer() or there is no more data in that direction
	 */
	public boolean shiftBuffer(boolean forward)
	{
		if(mRequest != null && !mRequest.isDone())
		{
			mRequest.cancel(true);
		}
		
		// Check that we have a date at all
		if(mBuffer.getStartTimestamp() == 0)
			return false;
		
		// TODO: Enable reading from active buffer
		if(forward)
		{
			// Check if there are anymore records
			if(mBuffer.getEndTimestamp() + 1 > mLogFile.getEndDate())
				return false;
			
			mRequestType = RequestType.ForwardShift;
			mRequestDate = mLogFile.getNextAvailableDateAfter(mBuffer.getEndTimestamp()) + 1;
			if(mRequestDate == 0)
				return false;
			mRequest = mLogFile.loadRecordChunksAsync(mRequestDate, mRequestDate);
		}
		else
		{
			// Check if there are anymore records
			if(mBuffer.getStartTimestamp() - 1 < mLogFile.getStartDate())
				return false;
			
			mRequestType = RequestType.BackwardShift;
			mRequestDate = mLogFile.getNextAvailableDateBefore(mBuffer.getStartTimestamp()) - 1;
			if(mRequestDate == 0)
				return false;
			mRequest = mLogFile.loadRecordChunksAsync(mRequestDate, mRequestDate);
		}
	
		return true;
	}
	
	/**
	 * Requests that the buffer be filled with records from the chunk around that date.
	 * @param date The date to include in the buffer
	 * @param true to indicate to focus on providing records after the date
	 * @return True if the request has been successfully made. False if there is no data available in that date region
	 */
	public boolean seekBuffer(long date, boolean forward)
	{
		if(mRequest != null && !mRequest.isDone())
		{
			mRequest.cancel(true);
		}
		LogUtil.finest("Seeking buffer");
		// TODO: Enable reading from active buffer
		if(forward)
		{
			
			// Check if there are any records
			if(date > mLogFile.getEndDate())
				return false;
				
			
			if(date < mLogFile.getStartDate())
				date = mLogFile.getStartDate();

			mRequestType = RequestType.ForwardSeek;
			mRequestDate = date;
			mRequest = mLogFile.loadRecordChunksAsync(date, date);
		}
		else
		{
			// Check if there are any records
			if(date < mLogFile.getStartDate())
				return false;
			
			if(date > mLogFile.getEndDate())
				date = mLogFile.getEndDate();

			mRequestType = RequestType.BackwardSeek;
			mRequestDate = date;
			mRequest = mLogFile.loadRecordChunksAsync(date, date);
		}
		
		return true;
	}
	
	/**
	 * You must call this so the buffer can be updated with retrieved records
	 * @return True if a waiting request finished
	 */
	public boolean update()
	{
		if(mRequest != null && mRequest.isDone())
		{
			// There is data to retrieve
			
			try
			{
				switch(mRequestType)
				{
				case ForwardShift:
					mBuffer.removeBefore(mBufferChunkBreak);
					mRelativeOffset += mBufferChunkBreak;
					
					mBufferChunkBreak = mBuffer.size();
					
					mBuffer.addAll(mRequest.get());
					
					mRequest = null;
					break;
				case BackwardShift:
				{
					RecordList temp = mRequest.get();
					mBufferChunkBreak = temp.size();
					
					mRelativeOffset -= temp.size();
					
					temp.addAll(mBufferChunkBreak, mBuffer);
					mBuffer = temp;
					
					mRequest = null;
					break;
				}
				case ForwardSeek:
					mBuffer.clear();
					mBuffer = mRequest.get();
					mBufferChunkBreak = 0;
					mRelativeOffset = 0;
					
					if(!shiftBuffer(true))
						mRequest = null;
				
					break;
				case BackwardSeek:
					mBuffer.clear();
					mBuffer = mRequest.get();
					mBufferChunkBreak = mBuffer.size();
					mRelativeOffset = 0;
					
					if(!shiftBuffer(false))
						mRequest = null;
					break;
				}
				
				LogUtil.finest("Buffer Shifted. Idx:" + mRelativeOffset);
			}
			catch(InterruptedException e)
			{
				// Ignore
			}
			catch(ExecutionException e)
			{
				e.printStackTrace();
			}
			
			return true;
		}
		return false;
	}

	public void release() 
	{
		mLogFile.closeAsync(false);
		mBuffer.clear();
		
	}

	public String getPlayer() 
	{
		return mLogFile.getName();
	}
	
	public LogFile getLogFile()
	{
		return mLogFile;
	}
}
