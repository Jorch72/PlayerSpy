package au.com.mineauz.PlayerSpy.search;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.PaintingChangeRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class BlockConstraint extends Constraint 
{
	public enum Type
	{
		Place,
		Break,
		Any
	}
	public Type type;
	public Pair<Material, Integer> material;
	
	@Override
	public String toString() 
	{
		return "{ type: " + type + ", material: " + material + "}"; 
	}

	@Override
	public boolean matches( Record record )
	{
		if(material.getArg1() == Material.PAINTING)
		{
			if(!(record instanceof PaintingChangeRecord))
				return false;
			
			PaintingChangeRecord change = (PaintingChangeRecord)record;
			
			if(type == Type.Place && !change.getPlaced() || type == Type.Break && change.getPlaced())
				return false;
			
			return true;
		}
		else
		{
			if(!(record instanceof BlockChangeRecord))
				return false;
			
			BlockChangeRecord change = (BlockChangeRecord)record;
			
			if(type == Type.Place && !change.wasPlaced() || type == Type.Break && change.wasPlaced())
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

	@Override
	public String getDescription()
	{
		String result = "Block ";
		switch(type)
		{
		case Any:
			result += "Changes";
			break;
		case Break:
			result += "Breaks";
			break;
		case Place:
			result += "Places";
			break;
		}
		
		if(material.getArg1() == Material.AIR)
			return result;
		
		result += " of ";
		result += Utility.formatItemName(new ItemStack(material.getArg1(),1,(short)(int)material.getArg2()));
		
		return result;
	}
}
