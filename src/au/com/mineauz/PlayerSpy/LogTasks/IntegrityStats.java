package au.com.mineauz.PlayerSpy.LogTasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class IntegrityStats
{
	public IntegrityStats() {}
	
	public int getLogCount()
	{
		return mAllLogs.size();
	}
	
	public int getCorruptLogCount()
	{
		return mCorruptLogs.size();
	}
	
	public List<String> getCorruptedLogs()
	{
		return Collections.unmodifiableList(mCorruptLogs);
	}
	
	public List<String> getAllLogs()
	{
		return Collections.unmodifiableList(mAllLogs);
	}
	
	public List<String> getOkLogs()
	{
		ArrayList<String> list = new ArrayList<String>(mAllLogs);
		list.removeAll(mCorruptLogs);
		return list;
	}
	
	public int getSessionCount()
	{
		return mTotalSessions;
	}
	
	public int getCorruptSessionCount()
	{
		return mTotalCorruptSessions;
	}
	
	public int getSessionCount(String file)
	{
		if(!mSessions.containsKey(file))
			return 0;
		
		return mSessions.get(file);
	}
	
	public int getCorruptSessionCount(String file)
	{
		if(!mCorruptSessions.containsKey(file))
			return 0;
		
		return mCorruptSessions.get(file);
	}
	
	int mTotalSessions = 0;
	int mTotalCorruptSessions = 0;
	
	ArrayList<String> mCorruptLogs = new ArrayList<String>();
	ArrayList<String> mAllLogs = new ArrayList<String>();
	HashMap<String, Integer> mSessions = new HashMap<String, Integer>();
	HashMap<String, Integer> mCorruptSessions = new HashMap<String, Integer>(); 
}