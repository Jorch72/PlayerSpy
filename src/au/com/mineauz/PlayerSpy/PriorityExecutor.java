package au.com.mineauz.PlayerSpy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import au.com.mineauz.PlayerSpy.LogTasks.Task;

public class PriorityExecutor implements Executor
{
	private static class SubmittedTask
	{
		public Future<?> future;
		public Task<?> task;
	}
	private static class ThreadInfo
	{
		public ExecutorService executor;
		public ArrayDeque<SubmittedTask> taskQueue;
		
		public synchronized void scheduleNext()
		{
			if(taskQueue.isEmpty())
				return;
			
			// Get the next task to do
			SubmittedTask nextTask = taskQueue.poll();
			while((nextTask.future.isCancelled() || nextTask.future.isDone()) && taskQueue.size() > 0)
				nextTask = taskQueue.poll();

			if(nextTask.future.isCancelled() || nextTask.future.isDone())
				return;
			
			final Future<?> toExecute = nextTask.future;
			
			executor.execute(new Runnable() 
			{
				@Override
				public void run() 
				{
					try
					{
						toExecute.get();
					} 
					catch (InterruptedException e) 
					{
						LogUtil.fine("Task was interupted");
					} 
					catch (ExecutionException e) 
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
			mThreadPool.add(info);
		}
	}
	
	@Override
	public void execute(Runnable command) 
	{
	}

	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	public List<Runnable> shutdownNow() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isShutdown() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isTerminated() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	public <T> Future<T> submit(Task<T> task) 
	{
		int taskId = task.getTaskTargetId();

		int best = -1;
		int bestWeight = Integer.MAX_VALUE;
		
		// Find the appropriate thread for the task
		int i = 0;
		for(ThreadInfo info : mThreadPool)
		{
			int weight = info.taskQueue.size();
			
			for(SubmittedTask sTask : info.taskQueue)
			{
				if(sTask.future.isCancelled())
					weight--;
				else if(sTask.task.getTaskTargetId() == taskId)
					weight = Integer.MIN_VALUE;
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
		
		boolean empty = mThreadPool.get(best).taskQueue.size() == 0;
		mThreadPool.get(best).taskQueue.add(sTask);
		
		// Start the thread executing if it needs to
		if(empty)
			mThreadPool.get(best).scheduleNext();
		
		return future;
	}
}
