package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;
import java.util.Date;

import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class DateConstraintDA extends DataAssembler 
{
	private boolean mBetween;
	public DateConstraintDA(boolean between)
	{
		mBetween = between;
	}
	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		Date startDate = new Date(0L);
		Date endDate = new Date(Long.MAX_VALUE);
		
		if(mBetween)
		{
			endDate = (Date)objects.pop();
			objects.pop();
			startDate = (Date)objects.pop();
			objects.pop();
		}
		else
		{
			Date temp = (Date) objects.pop();
			String type = (String)objects.pop();
			
			if(type.equals("after"))
				startDate = temp;
			else
				endDate = temp;
		}
		
		DateConstraint constraint = new DateConstraint();
		constraint.startDate = startDate;
		constraint.endDate = endDate;
		return constraint;
	}

}
