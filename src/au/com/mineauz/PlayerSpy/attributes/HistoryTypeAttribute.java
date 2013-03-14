package au.com.mineauz.PlayerSpy.attributes;

import java.util.HashMap;
import java.util.List;

import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Utilities.StringUtil;
import au.com.mineauz.PlayerSpy.search.AttackConstraint;
import au.com.mineauz.PlayerSpy.search.BlockChangeConstraint;
import au.com.mineauz.PlayerSpy.search.CompoundConstraint;
import au.com.mineauz.PlayerSpy.search.ItemTransactionConstraint;
import au.com.mineauz.PlayerSpy.search.RecordTypeConstraint;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class HistoryTypeAttribute extends SetAttribute<Constraint>
{

private static HashMap<String, Constraint> mTypeMapping;
	
	static
	{
		mTypeMapping = new HashMap<String, Constraint>();
		
		Constraint c = new RecordTypeConstraint(RecordType.ItemPickup);
		mTypeMapping.put("collect", c);
		mTypeMapping.put("pickup", c);
		
		mTypeMapping.put("drop", new RecordTypeConstraint(RecordType.DropItem));
		
		mTypeMapping.put("items", new CompoundConstraint(false, new RecordTypeConstraint(RecordType.ItemPickup), new RecordTypeConstraint(RecordType.DropItem)));
		
		c = new RecordTypeConstraint(RecordType.Interact);
		mTypeMapping.put("interact", c);
		mTypeMapping.put("use", c);
		mTypeMapping.put("interactions", c);
		mTypeMapping.put("uses", c);
		
		mTypeMapping.put("death", new RecordTypeConstraint(RecordType.Death));
		mTypeMapping.put("deaths", new RecordTypeConstraint(RecordType.Death));
		
		mTypeMapping.put("kill", new AttackConstraint(true)); // Special: only displays the kills
		mTypeMapping.put("kills", new AttackConstraint(true)); // Special: only displays the kills
		mTypeMapping.put("attack", new AttackConstraint(false)); // Special: only displays the attack part
		mTypeMapping.put("attacks", new AttackConstraint(false)); // Special: only displays the attack part
		
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
	
	public HistoryTypeAttribute()
	{
		super("type");
	}

	@Override
	protected int parseElement( String input, int start, List<Constraint> output ) throws IllegalArgumentException
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
