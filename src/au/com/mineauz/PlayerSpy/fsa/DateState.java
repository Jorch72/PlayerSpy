package au.com.mineauz.PlayerSpy.fsa;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayDeque;
import java.util.Date;

import au.com.mineauz.PlayerSpy.SpyPlugin;

public class DateState extends State
{
	@Override
	public boolean match(String word, ArrayDeque<Object> output) 
	{
		DateFormat fmt = DateFormat.getDateInstance(DateFormat.SHORT);
		fmt.setTimeZone(SpyPlugin.getSettings().timezone);
		try 
		{
			Date date = fmt.parse(word);
			output.push(date);
			return true;
		} 
		catch (ParseException e) 
		{
			return false;
		}
	}

	@Override
	public String getExpected() 
	{
		return "date";
	}

}
