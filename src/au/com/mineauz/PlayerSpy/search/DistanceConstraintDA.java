package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class DistanceConstraintDA extends DataAssembler 
{

	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		Integer distance = (Integer)objects.pop();
		objects.pop();
		
		DistanceConstraint constraint = new DistanceConstraint();
		constraint.distance = distance;
		
		return constraint;
	}

}
