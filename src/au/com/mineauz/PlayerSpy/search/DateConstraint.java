package au.com.mineauz.PlayerSpy.search;

import java.util.Date;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class DateConstraint extends Constraint 
{
	public Date startDate;
	public Date endDate;
	@Override
	public boolean matches( Record record )
	{
		if(record.getTimestamp() < startDate.getTime() || record.getTimestamp() > endDate.getTime())
			return false;
		
		return true;
	}
	@Override
	public String getDescription()
	{
		if (startDate.getTime() == 0)
			return "Before " + Utility.formatTime(endDate.getTime(), "dd/MM/yy HH:mm:ss");
		else if (endDate.getTime() == Long.MAX_VALUE)
			return "After " + Utility.formatTime(startDate.getTime(), "dd/MM/yy HH:mm:ss");
		else
			return "Between " + Utility.formatTime(startDate.getTime(), "dd/MM/yy HH:mm:ss") + " and " + Utility.formatTime(endDate.getTime(), "dd/MM/yy HH:mm:ss");
	}
}
