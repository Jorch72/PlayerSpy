package au.com.mineauz.PlayerSpy.attributes;

import java.util.ArrayList;
import java.util.List;

import au.com.mineauz.PlayerSpy.Utilities.Match;
import au.com.mineauz.PlayerSpy.Utilities.StringUtil;

public abstract class SetAttribute<T> extends Attribute
{
	public SetAttribute(String name)
	{
		super(name, AttributeValueType.String);
	}
	
	protected abstract int parseElement(String input, int start, List<T> output) throws IllegalArgumentException;
	
	@Override
	public Match matchNext( String input, int start ) throws IllegalArgumentException
	{
		int realStart = start;
		
		ArrayList<T> elements = new ArrayList<T>();
		if(input.charAt(start) == '[')
		{
			++start;
			start = StringUtil.getNextNonSpaceChar(input, start);
			
			boolean ended = false;
			boolean first = true;
			int lastStart = start;
			while (start < input.length())
			{
				if(input.charAt(start) == ']')
				{
					ended = true;
					++start;
					break;
				}
				
				if(!first)
				{
					StringUtil.validateExpected(input, start, ",", "Expected comma or ] after " + input.substring(lastStart,start));
					start = StringUtil.getNextNonSpaceChar(input, start+1);
				}
				
				lastStart = start;
				start = parseElement(input,start,elements);
				start = StringUtil.getNextNonSpaceChar(input, start);

				first = false;
			}
			
			if(!ended)
				throw new IllegalArgumentException("Type list was not finished.");
		}
		else
		{
			start = parseElement(input, start, elements);
		}
		
		return new Match(realStart, start, elements, this);
	}
}
