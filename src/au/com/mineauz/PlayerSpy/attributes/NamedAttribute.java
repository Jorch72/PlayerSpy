package au.com.mineauz.PlayerSpy.attributes;

import au.com.mineauz.PlayerSpy.Utilities.Match;
import au.com.mineauz.PlayerSpy.Utilities.StringUtil;

public class NamedAttribute extends Attribute
{
	private String[] mAliases;
	
	public NamedAttribute(String name, AttributeValueType type, String... aliases)
	{
		super(name, type);
		mAliases = aliases;
	}
	
	public Match matchNext(String input, int start) throws IllegalArgumentException
	{
		int nameStart = start;
		
		if(StringUtil.startsWithIgnoreCase(input, getName(), start))
			start += getName().length();
		else if(mAliases != null)
		{
			for(String alias : mAliases)
			{
				if(StringUtil.startsWithIgnoreCase(input,alias,start))
				{
					start += alias.length();
					break;
				}
			}
		}
		
		// No match
		if(start == nameStart)
			return null;
		
		// Skip any whitespace
		start = StringUtil.getNextNonSpaceChar(input, start);
				
		Match m = super.matchNext(input, start);
		
		if(m == null)
			return null;
		
		return new Match(nameStart, start, m.value, this);
	}
}
