package au.com.mineauz.PlayerSpy.Utilities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.bukkit.Bukkit;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.LogTasks.Task;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.debugging.Profiler;

public class PriorityExecutor
{
	private static class SubmittedTask<T>
	{
		public Future<T> future;
		public Task<T> task;
		public Callback<T> callback;
	}
	private static class ThreadInfo
	{
		public int id;
		
		public ExecutorService executor;
		public ArrayDeque<SubmittedTask<?>> taskQueue;
		public boolean isExecuting = false;
		public int executingTargetId = -1;
		public SubmittedTask<?> current;
		
		public synchronized void scheduleNext()
		{
			if(taskQueue.isEmpty())
			{
				Profiler.setValue("thread-" + id + "-queue", 0);
				isExecuting = false;
				return;
			}
			
			// Get the next task to do
			SubmittedTask<?> nextTask = taskQueue.poll();
			while((nextTask.future.isCancelled() || nextTask.future.isDone()) && taskQueue.size() > 0)
				nextTask = taskQueue.poll();

			Profiler.setValue("thread-" + id + "-queue", taskQueue.size() + (isExecuting ? 1 : 0));
			if(nextTask.future.isCancelled() || nextTask.future.isDone())
			{
				isExecuting = false;
				Profiler.setValue("thread-" + id + "-queue", taskQueue.size());
				return;
			}

			final SubmittedTask<?> task = nextTask;
			
			current = nextTask;
			
			Debug.finest("Executing task " + nextTask.task.getClass().getSimpleName());
			
			isExecuting = true;
			executingTargetId = nextTask.task.getTaskTargetId();
			
			executor.execute(new Runnable() 
			{
				@SuppressWarnings( "unchecked" )
				@Override
				public void run() 
				{
					try
					{
						((FutureTask<?>)task.future).run();
						Object data = ((FutureTask<?>)task.future).get();
						
						if(task.callback != null)
							Bukkit.getScheduler().callSyncMethod(SpyPlugin.getInstance(), CallbackProxy.callSuccess((Callback<Object>)task.callback, data));
						
						//toExecute.get();
					}
					catch(Throwable e)
					{
						e.printStackTrace();
						
						if(task.callback != null)
							Bukkit.getScheduler().callSyncMethod(SpyPlugin.getInstance(), CallbackProxy.callFailure((Callback<Object>)task.callback, e));
					}
					finally
					{
						scheduleNext();
					}
				}
			});
		}
	}
	ArrayList<ThreadInfo> mThreadPool;
	
	public PriorityExecutor(int poolSize)
	{
		// Build up the pool of threads
		mThreadPool = new ArrayList<PriorityExecutor.ThreadInfo>(poolSize);
		for(int i = 0; i < poolSize; i++)
		{
			ThreadInfo info = new ThreadInfo();
			info.executor = Executors.newSingleThreadExecutor();
			info.taskQueue = new ArrayDeque<PriorityExecutor.SubmittedTask<?>>();
			info.id = i;
			mThreadPool.add(info);
		}
	}
	
	public synchronized <T> Future<T> submit(Task<T> task, Callback<T> callback)
	{
		int taskId = task.getTaskTargetId();

		int best = -1;
		int bestWeight = Integer.MAX_VALUE;
		
		// Find the appropriate thread for the task
		int i = 0;
		for(ThreadInfo info : mThreadPool)
		{
			int weight = info.taskQueue.size() + (info.isExecuting ? 1 : 0);
			
			if(info.isExecuting && info.executingTargetId == taskId && taskId != -1)
				weight = -1000;
			for(SubmittedTask<?> sTask : info.taskQueue)
			{
				if(sTask.future.isCancelled())
					weight--;
				else if(sTask.task.getTaskTargetId() == taskId && taskId != -1)
					weight = -1000;
			}
			
			if(weight < bestWeight)
			{
				best = i;
				bestWeight = weight;
			}
			i++;
		}
		
		if(best == -1)
			throw new RuntimeException("Error assigning work thread. No threads available.");
		
		// Submit the task
		Future<T> future = new FutureTask<T>((Callable<T>)task);
		SubmittedTask<T> sTask = new SubmittedTask<T>();
		sTask.task = task;
		sTask.future = future;
		sTask.callback = callback;
		
		Profiler.setValue("thread-" + best + "-queue", mThreadPool.get(best).taskQueue.size() + (mThreadPool.get(best).isExecuting ? 1 : 0));
		Debug.info("" + task.getClass().getSimpleName() + " submitted to thread " + best + ". QS: " + (mThreadPool.get(best).taskQueue.size() + (mThreadPool.get(best).isExecuting ? 1 : 0)));
		
		boolean empty = mThreadPool.get(best).taskQueue.size() == 0 && !mThreadPool.get(best).isExecuting;
		mThreadPool.get(best).taskQueue.add(sTask);
		
		// Start the thread executing if it needs to
		if(empty)
			mThreadPool.get(best).scheduleNext();
		
		return future;
	}
	
	public synchronized <T> Future<T> submit(Task<T> task) 
	{
		return submit(task, null);
	}
	
	/**
	 * Waits for all tasks to be completed
	 */
	public void waitForAll()
	{
		for(ThreadInfo thread : mThreadPool)
		{
			if(thread.taskQueue.isEmpty() && thread.isExecuting && !thread.current.future.isCancelled() && !thread.current.future.isDone())
			{
				try
				{
					thread.current.future.get();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				Iterator<SubmittedTask<?>> it = thread.taskQueue.descendingIterator();
				while(it.hasNext())
				{
					SubmittedTask<?> task = it.next();
					
					if(!task.future.isDone() && !task.future.isCancelled())
					{
						try
						{
							task.future.get();
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
						break;
					}
				}
			}
		}
	}
	
	
	private static class CallbackProxy<T> implements Callable<Void>
	{
		private Callback<T> mCallback;
		private boolean mCallSuccess;
		private Object mData;
		
		private CallbackProxy() {}
		
		public static <T> CallbackProxy<T> callSuccess(Callback<T> callback, T data)
		{
			CallbackProxy<T> proxy = new CallbackProxy<T>();
			proxy.mCallback = callback;
			proxy.mCallSuccess = true;
			proxy.mData = data;
			
			return proxy;
		}
		
		public static <T> CallbackProxy<T> callFailure(Callback<T> callback, Throwable error)
		{
			CallbackProxy<T> proxy = new CallbackProxy<T>();
			proxy.mCallback = callback;
			proxy.mCallSuccess = false;
			proxy.mData = error;
			
			return proxy;
		}
		
		@SuppressWarnings( "unchecked" )
		@Override
		public Void call()
		{
			if(mCallSuccess)
				mCallback.onSuccess((T)mData);
			else
				mCallback.onFailure((Throwable)mData);
			
			return null;
		}
		
	}
}
