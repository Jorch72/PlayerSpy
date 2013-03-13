package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class TimeConstraint extends Constraint
{
	private long mTime;
	private boolean mAfter;
	
	public TimeConstraint(long time, boolean after)
	{
		mTime = time;
		mAfter = after;
	}
	
	@Override
	public boolean matches( Record record )
	{
		if(mAfter && record.getTimestamp() >= mTime)
			return true;
		else if(!mAfter && record.getTimestamp() <= mTime)
			return true;
		
		return false;
	}

	@Override
	public String getDescription()
	{
		return null;
	}

}
