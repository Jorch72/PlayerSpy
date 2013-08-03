package au.com.mineauz.PlayerSpy.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.Utilities.CharType;
import au.com.mineauz.PlayerSpy.search.interfaces.CauseConstraint;

public class FilterCauseConstraint extends CauseConstraint
{
	private Pattern mPattern;
	
	public FilterCauseConstraint(String pattern)
	{
		pattern = pattern.toLowerCase();
		// Escape all symbols
		for(int i = 0; i < pattern.length(); ++i)
		{
			char ch = pattern.charAt(i);
			if(CharType.get(ch) == CharType.Symbol)
			{
				if(ch != '.')
				{
					pattern = pattern.substring(0,i) + "\\" + pattern.substring(i);
					++i;
				}
			}
		}
		
		pattern = pattern.replaceAll("\\.", ".+?");
		
		mPattern = Pattern.compile(pattern);
	}
	
	@Override
	public boolean matches( Cause cause )
	{
		String matchStr = cause.friendlyName().toLowerCase();
		Matcher m1 = mPattern.matcher(matchStr);
		
		return m1.find();
	}

	@Override
	public String getDescription()
	{
		return null;
	}

}
