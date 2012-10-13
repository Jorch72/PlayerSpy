package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;
import java.util.Date;

import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class DateConstraintDA extends DataAssembler 
{
	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		Date startDate = new Date(0L);
		Date endDate = new Date(Long.MAX_VALUE);
		
		Date temp = (Date)objects.pop();
		
		String type = (String)objects.pop();
		if(type.equals("and"))
		{
			endDate = temp;
			startDate = (Date)objects.pop();
			objects.pop();
		}
		else if(type.equals("after"))
		{
			startDate = temp;
		}
		else if(type.equals("before"))
		{
			endDate = temp;
		}
		
		DateConstraint constraint = new DateConstraint();
		constraint.startDate = startDate;
		constraint.endDate = endDate;
		return constraint;
	}

}
