package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.Utilities.StringUtil;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class TypeAttribute extends Attribute
{
	private static HashMap<String, Constraint> mTypeMapping;
	
	static
	{
		mTypeMapping = new HashMap<String, Constraint>();
		
		Constraint c = new RecordTypeConstraint(RecordType.Login);
		mTypeMapping.put("login", c);
		mTypeMapping.put("logon", c);
		mTypeMapping.put("join", c);
		
		c = new RecordTypeConstraint(RecordType.Logoff);
		mTypeMapping.put("logout", c);
		mTypeMapping.put("logoff", c);
		mTypeMapping.put("quit", c);
		mTypeMapping.put("leave", c);
		
		c = new RecordTypeConstraint(RecordType.ItemPickup);
		mTypeMapping.put("collect", c);
		mTypeMapping.put("pickup", c);
		
		mTypeMapping.put("drop", new RecordTypeConstraint(RecordType.DropItem));
		
		c = new RecordTypeConstraint(RecordType.Interact);
		mTypeMapping.put("interact", c);
		mTypeMapping.put("use", c);
		
		mTypeMapping.put("chat", new ChatConstraint()); // Special: only displays the chat part
		mTypeMapping.put("command", new CommandConstraint()); // Special: only displays the command part

		mTypeMapping.put("death", new RecordTypeConstraint(RecordType.Death));
		
		mTypeMapping.put("kill", new AttackConstraint(true)); // Special: only displays the kills
		mTypeMapping.put("attack", new AttackConstraint(false)); // Special: only displays the attack part
		
		// Special: only displays the removes part
		c = new BlockChangeConstraint(true, false, true, true);
		mTypeMapping.put("remove", c);
		mTypeMapping.put("mine", c);
		mTypeMapping.put("dig", c); 
		
		mTypeMapping.put("place", new BlockChangeConstraint(false, true, true, true)); // Special: only displays the placements
		
		c = new RecordTypeConstraint(RecordType.Damage);
		mTypeMapping.put("damage", c);
		mTypeMapping.put("damaged", c);
		
		// Special: only the taking from inv
		c = new ItemTransactionConstraint(true, false);
		mTypeMapping.put("took", c);
		mTypeMapping.put("take", c);
		
		// Special: only the placing in inv
		c = new ItemTransactionConstraint(false, true);
		mTypeMapping.put("gave", c);
		mTypeMapping.put("put", c);
	}
	
	public TypeAttribute( )
	{
		super("type", AttributeValueType.String);
	}

	private int parseType(String input, int start, ArrayList<Constraint> output) throws IllegalArgumentException
	{
		String bestMatch = "";
		for(String name : mTypeMapping.keySet())
		{
			if(name.length() > bestMatch.length() && StringUtil.startsWithIgnoreCase(input, name, start))
				bestMatch = name;
		}
		
		if(bestMatch.isEmpty())
			throw new IllegalArgumentException("Unknown type starting at '" + (start + 10 <= input.length() ? input.substring(start, start+10) + "...'" : input.substring(start) + "'"));
		
		output.add(mTypeMapping.get(bestMatch));
		return start + bestMatch.length();
	}
	
	@Override
	public Match matchNext( String input, int start ) throws IllegalArgumentException
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
		
		if(start >= input.length())
			return null;
		
		ArrayList<Constraint> types = new ArrayList<Constraint>();
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
				start = parseType(input,start,types);
				start = StringUtil.getNextNonSpaceChar(input, start);

				first = false;
			}
			
			if(!ended)
				throw new IllegalArgumentException("Type list was not finished.");
		}
		else
		{
			start = parseType(input, start, types);
		}
		
		return new Match(realStart, start, new Pair<Set<Modifier>, Object>(applicableModifiers.keySet(),types), this);
	}
}
