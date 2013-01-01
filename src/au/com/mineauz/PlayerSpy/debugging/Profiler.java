package au.com.mineauz.PlayerSpy.debugging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map.Entry;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Utilities.Pair;

public class Profiler
{
	private static final int cHistorySize = 10000;
	
	private static HashMap<String, ArrayDeque<Pair<Long,Object>>> mWhatchedVariables = new HashMap<String, ArrayDeque<Pair<Long,Object>>>();
	private static HashMap<String, ArrayDeque<Long>> mTimingHistory = new HashMap<String, ArrayDeque<Long>>();
	private static HashMap<Long, ThreadData> mPerThreadData = new HashMap<Long, ThreadData>();
	
	public static void beginTimingSection(String name)
	{
		ThreadData data;
		if(mPerThreadData.containsKey(Thread.currentThread().getId()))
			data = mPerThreadData.get(Thread.currentThread().getId());
		else
		{
			data = new ThreadData();
			mPerThreadData.put(Thread.currentThread().getId(),data);
		}
		
		data.timings.push(new Pair<String, Long>(name, System.currentTimeMillis()));
	}
	public static void endTimingSection()
	{
		ThreadData data;
		if(mPerThreadData.containsKey(Thread.currentThread().getId()))
			data = mPerThreadData.get(Thread.currentThread().getId());
		else
			return;
		
		Pair<String, Long> section = data.timings.pop();
		
		// Build the path
		String sectionPath = section.getArg1();
		for(Pair<String,Long> val : data.timings)
			sectionPath = val.getArg1() + "." + sectionPath;
		
		synchronized ( mTimingHistory )
		{
			long timeDiff = System.currentTimeMillis() - section.getArg2();
			ArrayDeque<Long> queue;
			if(mTimingHistory.containsKey(sectionPath))
				queue = mTimingHistory.get(sectionPath);
			else
			{
				queue = new ArrayDeque<Long>();
				mTimingHistory.put(sectionPath, queue);
			}
			
			queue.offer(timeDiff);
			
			if(queue.size() > cHistorySize)
				queue.poll();
		}
	}
	public static void outputDebugData()
	{
		try
		{
			File path = new File(SpyPlugin.getInstance().getDataFolder(), "DebugLog.csv");
			FileWriter outputStream = new FileWriter(path);
			BufferedWriter output = new BufferedWriter(outputStream);

			output.write("Timings specific: \n");
			output.write("Key,Minimum (ms),Maximum (ms),Average (ms)\n");
			HashMap<String, ArrayDeque<Long>> totals = new HashMap<String, ArrayDeque<Long>>();
			
			synchronized ( mTimingHistory )
			{
				for(Entry<String, ArrayDeque<Long>> timings : mTimingHistory.entrySet())
				{
					long min,max,average;
					min = Long.MAX_VALUE;
					max = Long.MIN_VALUE;
					average = 0;
					
					for(Long time : timings.getValue())
					{
						min = Math.min(min, time);
						max = Math.max(max, time);
						
						average += time;
					}
					
					average /= timings.getValue().size();
					output.write(timings.getKey() + "," + min + "," + max + "," + average + "\n");
					
					// Do the totals
					String endPart = timings.getKey();
					if(endPart.contains("."))
						endPart = endPart.substring(endPart.lastIndexOf(".") + 1);
					
					ArrayDeque<Long> total;
					if(totals.containsKey(endPart))
						total = totals.get(endPart);
					else
					{
						total = new ArrayDeque<Long>();
						totals.put(endPart, total);
					}
					
					total.addAll(timings.getValue());
				}
			}
			
			output.write("\n\nTimings Total: \n");
			output.write("Key,Minimum (ms),Maximum (ms),Average (ms)\n");
			for(Entry<String, ArrayDeque<Long>> timings : totals.entrySet())
			{
				long min,max,average;
				min = Long.MAX_VALUE;
				max = Long.MIN_VALUE;
				average = 0;
				
				for(Long time : timings.getValue())
				{
					min = Math.min(min, time);
					max = Math.max(max, time);
					
					average += time;
				}
				
				average /= timings.getValue().size();
				output.write(timings.getKey() + "," + min + "," + max + "," + average + "\n");
			}
			
			output.write("\n\nWhatched Values: \n");
			synchronized ( mWhatchedVariables )
			{
				for(Entry<String, ArrayDeque<Pair<Long, Object>>> variable : mWhatchedVariables.entrySet())
				{
					output.write("\nVariable:," + variable.getKey()+"\n");
					output.write("Time (ms),Value\n");
					
					for(Pair<Long,Object> value : variable.getValue())
						output.write(value.getArg1()+"," + value.getArg2() + "\n");
				}
			}
			
			
			output.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void setValue(String name, Object value)
	{
		synchronized (mWhatchedVariables)
		{
			Pair<Long,Object> pair = new Pair<Long, Object>(System.currentTimeMillis(), value);
			
			ArrayDeque<Pair<Long,Object>> history;
			if(mWhatchedVariables.containsKey(name))
				history = mWhatchedVariables.get(name);
			else
			{
				history = new ArrayDeque<Pair<Long,Object>>();
				mWhatchedVariables.put(name,history);
			}
			
			history.offer(pair);
			
			if(history.size() > cHistorySize)
				history.poll();
		}
	}
	public static Object getValue(String name)
	{
		synchronized (mWhatchedVariables)
		{
			ArrayDeque<Pair<Long,Object>> history;
			if(mWhatchedVariables.containsKey(name))
				history = mWhatchedVariables.get(name);
			else
				return null;
			
			return history.peekLast().getArg2();
		}
	}
}

class ThreadData
{
	public ArrayDeque<Pair<String,Long>> timings = new ArrayDeque<Pair<String,Long>>();
}
