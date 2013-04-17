package au.com.mineauz.PlayerSpy.attributes;

import java.util.HashMap;
import java.util.List;

import au.com.mineauz.PlayerSpy.Utilities.StringUtil;
import au.com.mineauz.PlayerSpy.search.BlockChangeConstraint;
import au.com.mineauz.PlayerSpy.search.ItemTransactionConstraint;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class RollbackTypeAttribute extends Attribute
{

private static HashMap<String, Constraint> mTypeMapping;
	
	static
	{
		mTypeMapping = new HashMap<String, Constraint>();
		
		Constraint c = null;
		
		// Special: only displays the removes part
		c = new BlockChangeConstraint(true, false, true, true);
		mTypeMapping.put("remove", c);
		mTypeMapping.put("mine", c);
		mTypeMapping.put("dig", c); 
		
		mTypeMapping.put("place", new BlockChangeConstraint(false, true, true, true)); // Special: only displays the placements
		
		c = new BlockChangeConstraint(true, true, true, true);
		mTypeMapping.put("block", c);
		mTypeMapping.put("blocks", c);
		
		// Special: only the taking from inv
		c = new ItemTransactionConstraint(true, false);
		mTypeMapping.put("took", c);
		mTypeMapping.put("take", c);
		
		// Special: only the placing in inv
		c = new ItemTransactionConstraint(false, true);
		mTypeMapping.put("gave", c);
		mTypeMapping.put("put", c);
		
		c = new ItemTransactionConstraint(true, true);
		mTypeMapping.put("transaction", c);
		mTypeMapping.put("transactions", c);
	}
	
	public RollbackTypeAttribute()
	{
		super("type", AttributeValueType.Set);
	}

	@Override
	protected int parseElement( String input, int start, List<Object> output ) throws IllegalArgumentException
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

}
