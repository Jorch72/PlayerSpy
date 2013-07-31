package au.com.mineauz.PlayerSpy.LogTasks;

import java.util.concurrent.Callable;

import org.bukkit.Bukkit;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Utilities.ProgressReportReceiver;

public abstract class Task<T> implements Callable<T> 
{
	private ProgressReportReceiver<T> mReceiver;
	
	/**
	 * Gets an id used to group similar tasks together 
	 */
	public abstract int getTaskTargetId();
	
	public abstract Priority getTaskPriority();
	
	protected void reportProgress(T data)
	{
		if(mReceiver != null)
			Bukkit.getScheduler().callSyncMethod(SpyPlugin.getInstance(), ProgressProxy.report(mReceiver, data));
	}
	
	public void setProgressReceiver(ProgressReportReceiver<T> receiver)
	{
		mReceiver = receiver;
	}
	
	public enum Priority
	{
		Low(1),
		Normal(2),
		High(3),
		Critical(4);
		
		private int mLevel;
		
		Priority(int level)
		{
			mLevel = level;
		}
		
		public boolean isHigher(Priority other)
		{
			return mLevel > other.mLevel;
		}
	}
	
	private static class ProgressProxy<T> implements Callable<Void>
	{
		private ProgressReportReceiver<T> mCallback;
		private T mData;
		
		private ProgressProxy() {}
		
		public static <T> ProgressProxy<T> report(ProgressReportReceiver<T> reporter, T data)
		{
			ProgressProxy<T> proxy = new ProgressProxy<T>();
			proxy.mCallback = reporter;
			proxy.mData = data;
			
			return proxy;
		}
		
		@Override
		public Void call()
		{
			mCallback.onProgressReport(mData);
			return null;
		}
		
	}
}
