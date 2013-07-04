package au.com.mineauz.PlayerSpy.LogTasks;


import org.bukkit.entity.EntityType;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.AttackRecord;
import au.com.mineauz.PlayerSpy.Records.DamageRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;

public class SearchForDamageTask implements Task<Long>
{
	private final EntityType mEntityType;
	private final boolean mAttack;
	private final String mPlayerName;
	private final long mSearchStartDate;
	private final boolean mForward;
	private final LogFile mLogFile;
	
	public SearchForDamageTask(LogFile logFile, EntityType entType, boolean attack, String playerName, long searchStartDate, boolean forward)
	{
		mLogFile = logFile;
		mEntityType = entType;
		mAttack = attack;
		mPlayerName = playerName;
		mSearchStartDate = searchStartDate;
		mForward = forward;
	}

	@Override
	public Long call() throws Exception 
	{
		// Load up the initial set
		RecordList temp;
		long date = mSearchStartDate;
		
		if(date == 0)
			date = (mForward ? mLogFile.getStartDate() + 1 : mLogFile.getEndDate() - 1);
		
		temp = mLogFile.loadRecordChunks(date, date);
		int startIndex = (mForward ? temp.getNextRecordAfter(date) : temp.getLastRecordBefore(date));
		
		while(temp.size() > 0)
		{
			if(mForward)
			{
				for(int i = startIndex; i < temp.size(); i++)
				{
					if((temp.get(i).getType() == RecordType.Attack && mAttack) || (temp.get(i).getType() == RecordType.Damage && !mAttack))
					{
						if(matches(temp.get(i)))
							return temp.get(i).getTimestamp();
					}
				}
			}
			else
			{
				for(int i = startIndex; i >= 0; i--)
				{
					if((temp.get(i).getType() == RecordType.Attack && mAttack) || (temp.get(i).getType() == RecordType.Damage && !mAttack))
					{
						if(matches(temp.get(i)))
							return temp.get(i).getTimestamp();
					}
				}
			}
			
			if(Thread.interrupted())
				return 0L;
			
			// Load the next chunk
			if(mForward)
				date = mLogFile.getNextAvailableDateAfter(temp.getEndTimestamp());
			else
				date = mLogFile.getNextAvailableDateBefore(temp.getStartTimestamp());
			
			temp = mLogFile.loadRecordChunks(date, date);
			
			// position the cursor
			if(mForward)
				startIndex = 0;
			else
				startIndex = temp.size()-1;
		}
		
		// No record was found
		return 0L;
	}
	
	
	private boolean matches(Record record)
	{
		if(mAttack)
		{
			AttackRecord arecord = (AttackRecord)record;
			
			EntityType type = arecord.getDamagee().getEntityType();
			
			if(type == mEntityType)
			{
				if(type == EntityType.PLAYER && mPlayerName != null)
				{
					if(arecord.getDamagee().getPlayerName() == mPlayerName)
						return true;
				}
				else
					return true;
			}
		}
		else
		{
			DamageRecord drecord = (DamageRecord)record;
			
			if(drecord.getDamager() != null)
			{
				EntityType type = drecord.getDamager().getEntityType();
				
				if(type == mEntityType)
				{
					if(type == EntityType.PLAYER && mPlayerName != null)
					{
						if(drecord.getDamager().getPlayerName() == mPlayerName)
							return true;
					}
					else
						return true;
				}
			}
		}
		
		return false;
	}

	@Override
	public int getTaskTargetId()
	{
		return mLogFile.getName().hashCode();
	}
	
	@Override
	public au.com.mineauz.PlayerSpy.LogTasks.Task.Priority getTaskPriority()
	{
		return Priority.High;
	}
}
