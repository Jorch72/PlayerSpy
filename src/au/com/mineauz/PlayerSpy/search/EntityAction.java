package au.com.mineauz.PlayerSpy.search;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;

public class EntityAction extends Action
{
	public EntityType entityType;
	public OfflinePlayer player;
	
	public boolean spawn;
	
	@Override
	public String toString() 
	{
		return "{ spawn: " + spawn + ", " + (entityType == null ? "any entity" : entityType.getName()) + (entityType == EntityType.PLAYER ? " " + player.getName() : "" ) + " }";
	}
}
