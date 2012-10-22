package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import org.bukkit.Material;

import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class BlockConstraintDA extends DataAssembler
{
	@SuppressWarnings("unchecked")
	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		Pair<Material, Integer> mat = (Pair<Material, Integer>)objects.pop();
		String action = (String)objects.pop();
		
		BlockConstraint result = new BlockConstraint();
		result.placed = (action.equals("place"));
		result.material = mat;
		
		return result;
	}

}
