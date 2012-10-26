package au.com.mineauz.PlayerSpy.search;

import java.util.Date;

import au.com.mineauz.PlayerSpy.Records.Record;
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
}
