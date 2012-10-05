package au.com.mineauz.PlayerSpy.LogTasks;

import java.util.concurrent.Callable;

public interface Task<T> extends Callable<T> 
{
	/**
	 * Gets an id used to group similar tasks together 
	 */
	public int getTaskTargetId();
}
