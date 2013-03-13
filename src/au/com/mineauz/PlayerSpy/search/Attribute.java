package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.mineauz.PlayerSpy.Utilities.CharType;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.Utilities.StringUtil;
import au.com.mineauz.PlayerSpy.Utilities.Util;

public class Attribute implements IMatchable
{
	private String mName;
	private String[] mAliases;
	private AttributeValueType mType;
	
	protected List<Modifier> mPossibleModifiers;
	
	private boolean mSingle;

	public Attribute(String name, AttributeValueType type, String... aliases)
	{
		mName = name;
		mAliases = aliases;
		mType = type;
		mPossibleModifiers = new ArrayList<Modifier>();
		mSingle = true;
	}
	
	public Attribute setSingular(boolean single)
	{
		mSingle = single;
		return this;
	}
	
	public boolean isSingular()
	{
		return mSingle;
	}
	
	public String getName()
	{
		return mName;
	}
	public Attribute addModifier(Modifier mod)
	{
		mPossibleModifiers.add(mod);
		return this;
	}
	
	public Match matchNext(String input, int start) throws IllegalArgumentException
	{
		int realStart = start;
		
		Map<Modifier,Match> applicableModifiers = new HashMap<Modifier, Match>();
		// Try to match modifiers
		boolean foundMod = false;
		do
		{
			foundMod = false;
			for(Modifier mod : mPossibleModifiers)
			{
				Match m = mod.matchNext(input, start);
				if(m != null)
				{
					foundMod = true;
					if(applicableModifiers.containsKey(mod))
						throw new IllegalArgumentException("Modifier " + mod.getName() + " already specified at " + applicableModifiers.get(mod).startPosition);
					
					applicableModifiers.put(mod,m);
					
					start = m.endPosition;
					
					// Skip any whitespace
					start = StringUtil.getNextNonSpaceChar(input, start);
				}
			}
		}
		while (foundMod);
		
		int nameStart = start;
		
		if(StringUtil.startsWithIgnoreCase(input, mName, start))
			start += mName.length();
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
		
		int valueStart = start;
		Object valueObject = null;
		
		switch(mType)
		{
		case Date:
		{
			Match m = Util.parseDate(input.substring(start), 0, 0, 0);
			if(m == null)
				throw new IllegalArgumentException("Expected date after '" + input.substring(nameStart,valueStart) + "'");
			
			start = m.endPosition + start;
			valueObject = (Long)m.value;
			break;
		}
		case Number:
		{
			Double value = null;
			
			start = StringUtil.getNextNonDigitChar(input, start);
			
			try
			{
				value = Double.parseDouble(input.substring(valueStart, start));
			}
			catch(NumberFormatException e)
			{
				throw new IllegalArgumentException("Expected number after '" + input.substring(nameStart,valueStart) + "'");
			}
			
			valueObject = value;
			break;
		}
		case NumberRange:
		{
			Double value1 = null;
			Double value2 = null;
			
			start = StringUtil.getNextNonDigitChar(input, start);
			
			try
			{
				value1 = Double.parseDouble(input.substring(valueStart, start));
			}
			catch(NumberFormatException e)
			{
				throw new IllegalArgumentException("Expected range lower number after '" + input.substring(nameStart,valueStart) + "'");
			}
			
			// Skip any whitespace
			start = StringUtil.getNextNonSpaceChar(input, start);
			
			if(start >= input.length() || input.charAt(start) != '-')
				throw new IllegalArgumentException("Expected - at " + start);
			
			// Skip any whitespace
			start = StringUtil.getNextNonSpaceChar(input, start);
			
			valueStart = start;
			
			start = StringUtil.getNextNonDigitChar(input, start);
			
			try
			{
				value1 = Double.parseDouble(input.substring(valueStart, start));
			}
			catch(NumberFormatException e)
			{
				throw new IllegalArgumentException("Expected range upper number after -");
			}
			
			valueObject = new Pair<Double, Double>(value1, value2);
			break;
		}
		case Sentence:
		{
			if(start >= input.length())
				throw new IllegalArgumentException("Expected string after '" + input.substring(nameStart,start) + "'");
			
			String value = null;
			int type = 0;
			if(input.charAt(start) == '"')
			{
				type = 1;
				++start;
			}
			else if(input.charAt(start) == '\'')
			{
				type = 2;
				++start;
			}
			
			valueStart = start;
			if(start >= input.length())
				throw new IllegalArgumentException("Unfinished string started after '" + input.substring(nameStart,valueStart-1) + "'");
			
			for(; start < input.length(); ++start)
			{
				if((type == 0 && CharType.get(input.charAt(start)) == CharType.Whitespace) || 
					(type == 1 && input.charAt(start) == '"') ||
					(type == 2 && input.charAt(start) == '\''))
				{
					value = input.substring(valueStart, start);
					if(type != 0)
						++start;
					
					break;
				}
			}
			
			if(value == null && type == 0)
				value = input.substring(valueStart);
			else if(value == null)
				throw new IllegalArgumentException("Unfinished string started after '" + input.substring(nameStart,valueStart-1) + "'");
			
			valueObject = value;
			break;
		}
		case String:
		{
			String value = null;
			for(; start < input.length(); ++start)
			{
				if(CharType.get(input.charAt(start)) == CharType.Whitespace)
				{
					value = input.substring(valueStart, start);
					break;
				}
			}
			
			if(value == null)
				value = input.substring(valueStart);
			
			valueObject = value;
			break;
		}
		}
		
		return new Match(realStart, start, new Pair<Set<Modifier>, Object>(applicableModifiers.keySet(), valueObject), this);
	}
}
