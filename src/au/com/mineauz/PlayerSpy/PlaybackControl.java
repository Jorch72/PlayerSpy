package au.com.mineauz.PlayerSpy;

import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import au.com.mineauz.PlayerSpy.LogTasks.SearchForBlockChangeTask;
import au.com.mineauz.PlayerSpy.LogTasks.SearchForDamageTask;
import au.com.mineauz.PlayerSpy.LogTasks.SearchForEventTask;
import au.com.mineauz.PlayerSpy.LogTasks.SearchForItemTask;
import au.com.mineauz.PlayerSpy.Records.RecordType;

public class PlaybackControl 
{
	public enum PlaybackState
	{
		Stopped,
		Playing,
		Paused,
		Buffering
	}
	
	public PlaybackControl(RecordBuffer buffer, Callable<Void> seekCallback, Callable<Void> finishCallback, Callable<Void> searchFailCallback)
	{
		mBuffer = buffer;
		mIsPlaying = false;
		mPlaybackDate = 0L;
		mAbsoluteIndex = 0;
		
		mIsSeeking = false;
		mSeekDate = 0L;
		mActualDate = Calendar.getInstance().getTimeInMillis();
		
		mSeekCallback = seekCallback;
		mFinishCallback = finishCallback;
		mSearchFailCallback = searchFailCallback;
	}

	public void close()
	{
		stop();
		mBuffer.release();
	}
	public boolean play()
	{
		// Check if no date has been specified by seek
		if(mPlaybackDate == 0)
		{
			LogUtil.finer("Not Seeked");
			return false;
		}
		
		// Check if the buffer is empty and we are not loading
		if(mBuffer.currentBuffer().size() == 0 && !mBuffer.isLoading())
		{
			LogUtil.finer("No data has been loaded or requested");
			return false;
		}
		
		// Check if we have reached the end of the buffer and are not loading more
		if(getBufferIndex() >= mBuffer.currentBuffer().size() && !mBuffer.isLoading())
		{
			LogUtil.finer("End of data");
			return false;
		}
		
		mIsPlaying = true;
		return true;
	}
	public void stop()
	{
		mIsPlaying = false;
		mIsSeeking = false;
		
		mPlaybackDate = 0L;
		mAbsoluteIndex = 0;
	}
	public void pause()
	{
		mIsPlaying = false;
	}
	
	public PlaybackState getState()
	{
		if(mSearchTask != null && (!mSearchTask.isDone() && !mSearchTask.isCancelled()))
			return PlaybackState.Buffering;
			
		if(getBufferIndex() >= mBuffer.currentBuffer().size() && mBuffer.isLoading())
			return PlaybackState.Buffering;
		else if(mIsSeeking)
			return PlaybackState.Buffering;
		
		if(mIsPlaying)
			return PlaybackState.Playing;
		else
		{
			if(mPlaybackDate == 0)
				return PlaybackState.Stopped;
			else
				return PlaybackState.Paused;
		}
	}
	
	public boolean skip(long time)
	{
		if(time == 0)
		{
			// Goto next entry
			if(getBufferIndex()+1 < mBuffer.currentBuffer().size())
				mAbsoluteIndex++;
			
			mPlaybackDate = mBuffer.currentBuffer().get(getBufferIndex()).getTimestamp();
			
			onSeek();
			return true;
		}
		
		long date = mPlaybackDate + time;
		return seek(date);
	}
	
	public boolean seek(long date)
	{
		// Check to see if that date is loaded at the moment
		if(date >= mBuffer.currentBuffer().getStartTimestamp() && date <= mBuffer.currentBuffer().getEndTimestamp())
		{
			mAbsoluteIndex = mBuffer.relativeOffset() + mBuffer.currentBuffer().getNextRecordAfter(date);
			mPlaybackDate = mBuffer.currentBuffer().get(getBufferIndex()).getTimestamp();
			LogUtil.finest("Seeking inside current buffer");
			onSeek();
			return true;
		}
		if(mBuffer.seekBuffer(date, true))
		{
			mIsSeeking = true;
			mSeekDate = date;
			
			return true;
		}
		
		return false;
	}
	
	public long getPlaybackDate()
	{
		return mPlaybackDate;
	}
	public long getStartDate()
	{
		return mBuffer.getLogFile().getStartDate();
	}
	public long getEndDate()
	{
		return mBuffer.getLogFile().getEndDate();
	}
	
	public int getBufferIndex()
	{
		return mAbsoluteIndex - mBuffer.relativeOffset();
	}
	
	public int getAbsoluteIndex()
	{
		return mAbsoluteIndex;
	}
	
	public RecordList getBuffer()
	{
		return mBuffer.currentBuffer();
	}
	
	public String getPlayer()
	{
		return mBuffer.getPlayer();
	}
	
	public int getRelativeOffset() 
	{
		return mBuffer.relativeOffset();
	}
	
	public void update()
	{
		if(mBuffer.update())
		{
			if(mIsSeeking)
			{
				// Find the record in the buffer with the date specified
				mAbsoluteIndex = mBuffer.currentBuffer().getNextRecordAfter(mSeekDate) + mBuffer.relativeOffset();
				mIsSeeking = false;
				
				if(getBufferIndex() >= 0 && getBufferIndex() < mBuffer.currentBuffer().size())
				{
					mPlaybackDate = mBuffer.currentBuffer().get(getBufferIndex()).getTimestamp();
				}
				else
					mPlaybackDate = mBuffer.currentBuffer().getEndTimestamp();
				
				onSeek();
			}
		}

		// Do special seek updates
		if(mSearchTask != null)
		{
			if(mSearchTask.isDone())
			{
				try 
				{
					// Now that we have the result, seek to it
					Long date = mSearchTask.get();
					
					if(date == 0L)
					{
						onSearchFail();
					}
					else
					{
						if(!seek(date - 1))
							onSearchFail();
					}
					
					mSearchTask = null;
				} 
				catch (InterruptedException e) 
				{
				} 
				catch (ExecutionException e) 
				{
					e.printStackTrace();
					onSearchFail();
				}
			}
		}
		else
		{
			// Dont do updates when waiting for a seek to finish
			if(!mIsSeeking)
			{
				// Try to get more data if needed
				if(mBuffer.shouldRequestMore(getBufferIndex(), true))
					mBuffer.shiftBuffer(true);
				
				// Do updates
				if(mIsPlaying)
				{

					// are we out of data?
					if(getBufferIndex() >= mBuffer.currentBuffer().size() - 1)
					{
						if(!mBuffer.isLoading())
						{
							// We are out of data and there is no more to get
							mIsPlaying = false;
							onFinish();
						}
					}
					else
					{
						long timeDiff = (long)((Calendar.getInstance().getTimeInMillis() - mActualDate) * mPlaybackSpeed);
						
						for(int i = getBufferIndex(); i < mBuffer.currentBuffer().size(); i++)
						{
							if(mBuffer.currentBuffer().get(i).getTimestamp() > mPlaybackDate + timeDiff)
								break;
							
								
							mAbsoluteIndex = i + mBuffer.relativeOffset();
							//mPlaybackDate = mBuffer.currentBuffer().get(i).getTimestamp();
						}
						mPlaybackDate += timeDiff;
						//LogUtil.("Running. Date: " + Util.dateToString(mPlaybackDate) + " @" + mAbsoluteIndex);
					}
				}
			}
		}
		mActualDate = Calendar.getInstance().getTimeInMillis();
	}
	
	public boolean seekToBlock(Material block, boolean mined, long date, boolean before) 
	{
		if(mSearchTask != null)
		{
			mSearchTask.cancel(true);
			mSearchTask = null;
		}
		
		mSearchTask = mBuffer.getLogFile().submitTask(new SearchForBlockChangeTask(mBuffer.getLogFile(), block, mined, date, !before));
		if(mSearchTask != null)
			return true;
		
		return false;
	}

	public boolean seekToItem(Material item, boolean gained, long date, boolean before) 
	{
		if(mSearchTask != null)
		{
			mSearchTask.cancel(true);
			mSearchTask = null;
		}
		
		mSearchTask = mBuffer.getLogFile().submitTask(new SearchForItemTask(mBuffer.getLogFile(), item, gained, date, !before));
		if(mSearchTask != null)
			return true;
		
		return false;
	}

	public boolean seekToEvent(RecordType type, long date, boolean before) 
	{
		if(mSearchTask != null)
		{
			mSearchTask.cancel(true);
			mSearchTask = null;
		}
		
		mSearchTask = mBuffer.getLogFile().submitTask(new SearchForEventTask(mBuffer.getLogFile(), type, date, !before));
		if(mSearchTask != null)
			return true;
		
		return false;
	}
	public boolean seekToDamage(EntityType entType, boolean attack, String playerName, long date, boolean before)
	{
		if(mSearchTask != null)
		{
			mSearchTask.cancel(true);
			mSearchTask = null;
		}
		
		mSearchTask = mBuffer.getLogFile().submitTask(new SearchForDamageTask(mBuffer.getLogFile(), entType, attack, playerName, date, !before));
		if(mSearchTask != null)
			return true;
		
		return false;
	}
	
	private void onSeek()
	{
		try
		{
			if(mSeekCallback != null)
				mSeekCallback.call();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void onFinish()
	{
		try
		{
			if(mFinishCallback != null)
				mFinishCallback.call();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void onSearchFail()
	{
		try
		{
			if(mSearchFailCallback != null)
				mSearchFailCallback.call();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private float mPlaybackSpeed = 1F;
	private RecordBuffer mBuffer;
	private boolean mIsPlaying;
	
	private boolean mIsSeeking;
	private long mSeekDate;
	
	private long mPlaybackDate;
	private long mActualDate;
	private int mAbsoluteIndex;
	
	private Callable<Void> mSeekCallback;
	private Callable<Void> mFinishCallback;
	private Callable<Void> mSearchFailCallback;
	
	private Future<Long> mSearchTask;
}
