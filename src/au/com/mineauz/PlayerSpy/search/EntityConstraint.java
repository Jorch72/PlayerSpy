package au.com.mineauz.PlayerSpy.search;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;

import au.com.mineauz.PlayerSpy.Records.AttackRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class EntityConstraint extends Constraint
{
	public EntityType entityType;
	public OfflinePlayer player;
	
	public boolean spawn;
	
	@Override
	public String toString() 
	{
		return "{ spawn: " + spawn + ", " + (entityType == null ? "any entity" : entityType.getName()) + (entityType == EntityType.PLAYER ? " " + player.getName() : "" ) + " }";
	}

	@Override
	public boolean matches( Record record )
	{
		if(spawn)
			return false; // There are no records for spawning entities
		
		if(record.getType() != RecordType.Attack || ((AttackRecord)record).getDamage() != -1)
			return false;
		
		if(entityType == null)
			return true;
		
		if(entityType != ((AttackRecord)record).getDamagee().getEntityType())
			return false;
		
		if(entityType == EntityType.PLAYER && !player.getName().equalsIgnoreCase(((AttackRecord)record).getDamagee().getPlayerName()))
			return false;
		
		
		return true;
	}
}
