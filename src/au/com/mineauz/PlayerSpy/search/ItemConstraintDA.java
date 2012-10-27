package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import org.bukkit.Material;

import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class ItemConstraintDA extends DataAssembler
{

	@SuppressWarnings( "unchecked" )
	@Override
	public Object assemble( ArrayDeque<Object> objects )
	{
		Object temp = objects.pop();
		ItemConstraint constraint = new ItemConstraint();
		
		if(temp instanceof Integer)
		{
			constraint.amount = (Integer)temp;
			temp = objects.pop();
		}
		else
			constraint.amount = 0;
		
		constraint.material = (Pair<Material,Integer>)temp;
		
		if(((String)objects.pop()).equalsIgnoreCase("pickup"))
			constraint.pickup = true;
		else
			constraint.pickup = false;
		
		return constraint;
	}

}
