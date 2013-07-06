package au.com.mineauz.PlayerSpy.attributes;

import java.util.HashMap;
import java.util.List;

import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Utilities.StringUtil;
import au.com.mineauz.PlayerSpy.search.AttackConstraint;
import au.com.mineauz.PlayerSpy.search.BlockChangeConstraint;
import au.com.mineauz.PlayerSpy.search.ChatConstraint;
import au.com.mineauz.PlayerSpy.search.CommandConstraint;
import au.com.mineauz.PlayerSpy.search.ItemTransactionConstraint;
import au.com.mineauz.PlayerSpy.search.RecordTypeConstraint;
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
		
		mTypeMapping.put("spawn", new RecordTypeConstraint(RecordType.EntitySpawn));
	}
	
	public TypeAttribute( )
	{
		super("type", AttributeValueType.Set);
	}

	@Override
	protected int parseElement(String input, int start, List<Object> output) throws IllegalArgumentException
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
