package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class InteractConstraintDA extends DataAssembler
{

	@SuppressWarnings( "unchecked" )
	@Override
	public Object assemble( ArrayDeque<Object> objects )
	{
		InteractConstraint constraint = new InteractConstraint();
		
		Object temp = objects.pop();
		
		if(temp instanceof Short) // entity id
		{
			if(((Short)temp) != -1)
				constraint.entity = EntityType.fromId((Short)temp);
			objects.pop();
		}
		else if (temp instanceof Pair<?,?>) // Type id
		{
			constraint.material = (Pair<Material, Integer>)temp;
			objects.pop();
		}
		else if(temp instanceof String && ((String)temp).equalsIgnoreCase("any"))
		{
			constraint.any = true;
			objects.pop();
		}
		else
			constraint.any = true;
		
		objects.pop();
		
		return constraint;
	}

}
