package au.com.mineauz.PlayerSpy.search;

import org.bukkit.Material;

import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Pair;

public class BlockConstraint extends Constraint 
{
	public boolean placed;
	public Pair<Material, Integer> material;
	
	@Override
	public String toString() 
	{
		return "{ placed: " + placed + ", material: " + material + "}"; 
	}

	@Override
	public boolean matches( Record record )
	{
		if(!(record instanceof BlockChangeRecord))
			return false;
		
		BlockChangeRecord change = (BlockChangeRecord)record;
		
		if(change.wasPlaced() != placed)
			return false;
		
		if(material.getArg1() == Material.AIR)
			return true;
		
		if(!change.getBlock().getType().equals(material.getArg1()))
			return false;
		
		if(material.getArg2() == -1)
			return true;
		
		if(change.getBlock().getData() == material.getArg2())
			return true;
		
		return false;
	}
}
