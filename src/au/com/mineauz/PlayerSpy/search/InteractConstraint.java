package au.com.mineauz.PlayerSpy.search;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import au.com.mineauz.PlayerSpy.Records.InteractRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class InteractConstraint extends Constraint
{
	public Pair<Material, Integer> material;
	public EntityType entity;
	public boolean any = false;
	
	@Override
	public boolean matches( Record record )
	{
		if(!(record instanceof InteractRecord))
			return false;
		
		InteractRecord interact = (InteractRecord)record;
		
		if(any)
			return true;
		
		if(material != null)
		{
			if(!interact.hasBlock())
				return false;
			
			if(material.getArg1() == Material.AIR)
				return true;
			
			if(material.getArg1() == interact.getBlock().getType())
			{
				if(material.getArg2() == -1)
					return true;
				if(material.getArg2() == interact.getBlock().getData())
					return true;
			}
		}
		else
		{
			if(!interact.hasEntity())
				return false;
			
			if(entity == null)
				return true;
			
			if(entity == interact.getEntity().getEntityType())
				return true;
		}
		return false;
		
	}

}
