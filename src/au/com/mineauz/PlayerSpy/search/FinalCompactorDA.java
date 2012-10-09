package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class FinalCompactorDA extends DataAssembler 
{

	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		SearchFilter filter = new SearchFilter();
		while(objects.size() != 0)
		{
			Object front = objects.pop();
			
			if(front instanceof Constraint)
				filter.constraints.add((Constraint)front);
			else if(front instanceof Action)
				filter.action = (Action)front;
		}
		
		return filter;
	}

}
