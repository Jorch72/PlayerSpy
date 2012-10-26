package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class ShowLocationDA extends DataAssembler
{

	@Override
	public Object assemble( ArrayDeque<Object> objects )
	{
		objects.pop();
		objects.pop();
		
		return new ShowLocationModifier();
	}

}
