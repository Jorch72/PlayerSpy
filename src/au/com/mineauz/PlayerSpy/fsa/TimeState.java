package au.com.mineauz.PlayerSpy.fsa;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;

import au.com.mineauz.PlayerSpy.SpyPlugin;

public class TimeState extends State 
{

	@Override
	public boolean match(String word, ArrayDeque<Object> output) 
	{
		// Try hh:mm:ssa
		try
		{
			DateFormat fmt = new SimpleDateFormat("hh:mm:ssa");
			fmt.setTimeZone(SpyPlugin.getSettings().timezone);
			Date date = fmt.parse(word);
			date.setTime(date.getTime() + SpyPlugin.getSettings().timeoffset);
			
			output.push(date);
			return true;
		}
		catch(ParseException e)
		{
		}
		// Try HH:mm:ss
		try
		{
			DateFormat fmt = new SimpleDateFormat("HH:mm:ss");
			fmt.setTimeZone(SpyPlugin.getSettings().timezone);
			Date date = fmt.parse(word);
			date.setTime(date.getTime() + SpyPlugin.getSettings().timeoffset);
			
			output.push(date);
			return true;
		}
		catch(ParseException e)
		{
		}
		try
		{
			DateFormat fmt = new SimpleDateFormat("hh:mma");
			fmt.setTimeZone(SpyPlugin.getSettings().timezone);
			Date date = fmt.parse(word);
			date.setTime(date.getTime() + SpyPlugin.getSettings().timeoffset);
			
			output.push(date);
			return true;
		}
		catch(ParseException e)
		{
		}
		try
		{
			DateFormat fmt = new SimpleDateFormat("HH:mm");
			fmt.setTimeZone(SpyPlugin.getSettings().timezone);
			Date date = fmt.parse(word);
			date.setTime(date.getTime() + SpyPlugin.getSettings().timeoffset);
			
			output.push(date);
			return true;
		}
		catch(ParseException e)
		{
		}
		
		return false;
	}

	@Override
	public String getExpected() 
	{
		return "time";
	}

}
