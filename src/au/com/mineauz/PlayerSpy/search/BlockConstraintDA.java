package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import org.bukkit.Material;

import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.fsa.DataAssembler;
import au.com.mineauz.PlayerSpy.search.BlockConstraint.Type;

public class BlockConstraintDA extends DataAssembler
{
	@SuppressWarnings("unchecked")
	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		Pair<Material, Integer> mat = (Pair<Material, Integer>)objects.pop();
		String action = (String)objects.pop();
		
		BlockConstraint result = new BlockConstraint();
		if(action.equals("place"))
			result.type = Type.Place;
		else if(action.equals("break"))
			result.type = Type.Break;
		else
			result.type = Type.Any;
		result.material = mat;
		
		return result;
	}

}
