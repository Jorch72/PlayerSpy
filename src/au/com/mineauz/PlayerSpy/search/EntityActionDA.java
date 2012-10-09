package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;

import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class EntityActionDA extends DataAssembler 
{
	private boolean mHasPlayer = false;
	public EntityActionDA()
	{
		
	}
	public EntityActionDA(boolean hasPlayer)
	{
		mHasPlayer = hasPlayer;
	}
	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		OfflinePlayer player;
		EntityType type;
		if(mHasPlayer)
		{
			player = (OfflinePlayer)objects.pop();
			type = EntityType.PLAYER;
		}
		else
		{
			short value = (Short)objects.pop();
			if(value == -1)
				type = null;
			else
				type = EntityType.fromId(value);
			player = null;
		}
		String actionString = (String)objects.pop();
		
		EntityAction action = new EntityAction();
		action.entityType = type;
		action.player = player;
		action.spawn = (actionString.equals("spawn"));
		
		return action;
	}

}
