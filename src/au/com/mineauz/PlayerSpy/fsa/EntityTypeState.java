package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

import org.bukkit.entity.EntityType;

import au.com.mineauz.PlayerSpy.Util;

public class EntityTypeState extends State
{
	@Override
	public boolean match(String word, ArrayDeque<Object> output) 
	{
		EntityType type = Util.parseEntity(word);
		
		if(type == null)
			return false;
		
		output.push(type.getTypeId());
		return true;
	}

	@Override
	public String getExpected() 
	{
		return "entity type";
	}

}
