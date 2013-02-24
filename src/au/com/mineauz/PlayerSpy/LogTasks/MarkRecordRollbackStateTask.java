package au.com.mineauz.PlayerSpy.LogTasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import au.com.mineauz.PlayerSpy.IndexEntry;
import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;

public class MarkRecordRollbackStateTask implements Task<Void>
{
	private ArrayList<Record> mToMark;
	private boolean mState;
	
	public MarkRecordRollbackStateTask(ArrayList<Record> toMark, boolean state)
	{
		mToMark = toMark;
		mState = state;
	}
	@Override
	public Void call() throws Exception
	{
		// Extract the list of logs to use
		HashSet<LogFile> logs = new HashSet<LogFile>();
		HashMap<String, HashMap<Integer, ArrayList<Short>>> breakDown = new HashMap<String, HashMap<Integer, ArrayList<Short>>>();
		
		for(Record record : mToMark)
		{
			if(record.sourceFile == null || record.sourceEntry == null)
				continue;
			
			logs.add(record.sourceFile);
			
			HashMap<Integer, ArrayList<Short>> logBreakdown;
			if(breakDown.containsKey(record.sourceFile.getName()))
				logBreakdown = breakDown.get(record.sourceFile.getName());
			else
			{
				logBreakdown = new HashMap<Integer, ArrayList<Short>>();
				breakDown.put(record.sourceFile.getName(), logBreakdown);
			}
			
			ArrayList<Short> sessionBreakdown;
			
			if(logBreakdown.containsKey(record.sourceEntry.Id))
				sessionBreakdown = logBreakdown.get(record.sourceEntry.Id);
			else
			{
				sessionBreakdown = new ArrayList<Short>();
				logBreakdown.put(record.sourceEntry.Id, sessionBreakdown);
			}
			
			sessionBreakdown.add(record.sourceIndex);
		}
		
		HashSet<LogFile> freshLogs = new HashSet<LogFile>();
		// Make sure all logs are loaded
		Iterator<LogFile> it = logs.iterator();
		while(it.hasNext())
		{
			LogFile log = it.next();
			if(!log.isLoaded() && !log.isTimingOut())
			{
				it.remove();
				freshLogs.add(LogFileRegistry.getLogFile(log.getName()));
			}
			else
			{
				log.addReference();
			}
		}
		
		logs.addAll(freshLogs);
		
		// Do the marking
		for(LogFile log : logs)
		{
			HashMap<Integer, ArrayList<Short>> logBreakdown = breakDown.get(log.getName());
			
			for(Entry<Integer, ArrayList<Short>> entry : logBreakdown.entrySet())
			{
				IndexEntry session = log.getSessionById(entry.getKey());
				log.setRollbackState(session, entry.getValue(), mState);
			}
		}
		return null;
	}

	@Override
	public int getTaskTargetId()
	{
		return -1;
	}

}
