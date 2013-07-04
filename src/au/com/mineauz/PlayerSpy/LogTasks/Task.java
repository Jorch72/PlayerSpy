package au.com.mineauz.PlayerSpy.LogTasks;

import java.util.concurrent.Callable;

public interface Task<T> extends Callable<T> 
{
	/**
	 * Gets an id used to group similar tasks together 
	 */
	public int getTaskTargetId();
	
	public Priority getTaskPriority();
	
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
}
