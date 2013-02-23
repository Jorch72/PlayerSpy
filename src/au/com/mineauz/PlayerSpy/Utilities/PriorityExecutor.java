package au.com.mineauz.PlayerSpy.Utilities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import au.com.mineauz.PlayerSpy.LogTasks.Task;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.debugging.Profiler;

public class PriorityExecutor
{
	private static class SubmittedTask
	{
		public Future<?> future;
		public Task<?> task;
	}
	private static class ThreadInfo
	{
		public int id;
		
		public ExecutorService executor;
		public ArrayDeque<SubmittedTask> taskQueue;
		public boolean isExecuting = false;
		public int executingTargetId = -1;
		
		public synchronized void scheduleNext()
		{
			if(taskQueue.isEmpty())
			{
				Profiler.setValue("thread-" + id + "-queue", 0);
				isExecuting = false;
				return;
			}
			
			// Get the next task to do
			SubmittedTask nextTask = taskQueue.poll();
			while((nextTask.future.isCancelled() || nextTask.future.isDone()) && taskQueue.size() > 0)
				nextTask = taskQueue.poll();

			Profiler.setValue("thread-" + id + "-queue", taskQueue.size() + (isExecuting ? 1 : 0));
			if(nextTask.future.isCancelled() || nextTask.future.isDone())
			{
				isExecuting = false;
				Profiler.setValue("thread-" + id + "-queue", taskQueue.size());
				return;
			}
			
			final Future<?> future = nextTask.future;
			
			Debug.finest("Executing task " + nextTask.task.getClass().getSimpleName());
			
			isExecuting = true;
			executingTargetId = nextTask.task.getTaskTargetId();
			
			executor.execute(new Runnable() 
			{
				@Override
				public void run() 
				{
					try
					{
						((FutureTask<?>)future).run();
						((FutureTask<?>)future).get();
						
						//toExecute.get();
					}
					catch(Exception e)
					{
						e.printStackTrace();
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
			info.taskQueue = new ArrayDeque<PriorityExecutor.SubmittedTask>();
			info.id = i;
			mThreadPool.add(info);
		}
	}
	
	public synchronized <T> Future<T> submit(Task<T> task) 
	{
		int taskId = task.getTaskTargetId();

		int best = -1;
		int bestWeight = Integer.MAX_VALUE;
		
		// Find the appropriate thread for the task
		int i = 0;
		for(ThreadInfo info : mThreadPool)
		{
			int weight = info.taskQueue.size() + (info.isExecuting ? 1 : 0);
			
			if(info.isExecuting && info.executingTargetId == taskId)
				weight = -1000;
			for(SubmittedTask sTask : info.taskQueue)
			{
				if(sTask.future.isCancelled())
					weight--;
				else if(sTask.task.getTaskTargetId() == taskId)
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
		SubmittedTask sTask = new SubmittedTask();
		sTask.task = task;
		sTask.future = future;
		
		Profiler.setValue("thread-" + best + "-queue", mThreadPool.get(best).taskQueue.size() + (mThreadPool.get(best).isExecuting ? 1 : 0));
		Debug.info("" + task.getClass().getSimpleName() + " submitted to thread " + best + ". QS: " + (mThreadPool.get(best).taskQueue.size() + (mThreadPool.get(best).isExecuting ? 1 : 0)));
		
		boolean empty = mThreadPool.get(best).taskQueue.size() == 0 && !mThreadPool.get(best).isExecuting;
		mThreadPool.get(best).taskQueue.add(sTask);
		
		// Start the thread executing if it needs to
		if(empty)
			mThreadPool.get(best).scheduleNext();
		
		return future;
	}
}
