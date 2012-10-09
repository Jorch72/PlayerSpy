package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;
import java.util.Date;

import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class DateCompactorDA extends DataAssembler 
{

	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		Date time = (Date)objects.poll();
		Date date = (Date)objects.poll();
		
		date = new Date(date.getTime() + time.getTime());
		return date;
	}

}
