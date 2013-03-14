package au.com.mineauz.PlayerSpy.attributes;

import au.com.mineauz.PlayerSpy.Utilities.IMatchable;
import au.com.mineauz.PlayerSpy.Utilities.Match;
import au.com.mineauz.PlayerSpy.Utilities.StringUtil;

public class Modifier implements IMatchable
{
	private final String mName;
	private final String[] mAliases;
	
	public Modifier(String name, String... aliases)
	{
		mName = name;
		mAliases = aliases;
	}
	
	public String getName()
	{
		return mName;
	}
	

	@Override
	public Match matchNext( String input, int start ) throws IllegalArgumentException
	{
		int realStart = start;
		
		if(StringUtil.startsWithIgnoreCase(input, mName, start))
			start += mName.length();
		else if(mAliases != null)
		{
			for(String alias : mAliases)
			{
				if(StringUtil.startsWithIgnoreCase(input, alias, start))
				{
					start += alias.length();
					break;
				}
			}
		}
		
		// No match
		if(start == realStart)
			return null;
		
		return new Match(realStart, start, null, this);
	}

}
