package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;
import java.util.Date;

import au.com.mineauz.PlayerSpy.Utility;
import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class TimeOnlyDA extends DataAssembler
{

	@Override
	public Object assemble( ArrayDeque<Object> objects )
	{
		Date date = (Date)objects.pop();
		
		// it only has the time portion, so add the date portion to it
		long datePart = Utility.getDatePortion(System.currentTimeMillis());
		
		datePart += date.getTime();
		
		return new Date(datePart);
	}

}
