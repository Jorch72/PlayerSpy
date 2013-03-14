package au.com.mineauz.PlayerSpy.attributes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import au.com.mineauz.PlayerSpy.Utilities.Match;
import au.com.mineauz.PlayerSpy.Utilities.StringUtil;

public class AttributeParser
{
	private Set<Attribute> mAttributes = new HashSet<Attribute>();
	
	public AttributeParser()
	{
		
	}
	
	public void addAttribute(Attribute attribute)
	{
		mAttributes.add(attribute);
	}
	
	@SuppressWarnings( "unchecked" )
	public List<ParsedAttribute> parse(String input) throws IllegalArgumentException
	{
		int start = 0;
		
		ArrayList<ParsedAttribute> results = new ArrayList<ParsedAttribute>();
		
		// Skip any whitespace
		start = StringUtil.getNextNonSpaceChar(input, start);
				
		HashSet<Attribute> foundAttributes = new HashSet<Attribute>();
		IllegalArgumentException lastError = null;
		
		while (start < input.length())
		{
			boolean matched = false;
			for(Attribute attr : mAttributes)
			{
				Match m;
				Set<Modifier> modifiers = new HashSet<Modifier>();
				try
				{
					m = attr.matchModifiers(input, start);
					
					int pos = start;
					if(m != null)
					{
						modifiers = (Set<Modifier>)m.value;
						pos = m.endPosition;
					}
					
					m = attr.matchNext(input, pos);
					
					if(m == null)
						continue;
					
				}
				catch(IllegalArgumentException e)
				{
					lastError = e;
					continue;
				}
					
				if(foundAttributes.contains(attr) && attr.isSingular())
					throw new IllegalArgumentException("Attribute " + attr.getName() + " was aready specified");
				
				matched = true;
				
				start = m.endPosition;
				ParsedAttribute res = new ParsedAttribute();
				res.source = attr;
				res.value = m.value;
				res.appliedModifiers = modifiers;
				
				results.add(res);
				foundAttributes.add(attr);
				break;
				
			}
			
			if(!matched)
			{
				if(lastError != null)
					throw lastError;
				
				break;
			}
			
			// Skip any whitespace
			start = StringUtil.getNextNonSpaceChar(input, start);
		}
		
		if(start < input.length()) // There were unmatched things in the input string
			throw new IllegalArgumentException("Unknown attribute at '" + input.substring(start) + "'");
		
		return results;
	}
	
	public static class ParsedAttribute
	{
		public Attribute source;
		public Set<Modifier> appliedModifiers;
		public Object value;
	}
}