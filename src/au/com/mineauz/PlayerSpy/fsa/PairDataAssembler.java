package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

import au.com.mineauz.PlayerSpy.Utilities.Pair;

public class PairDataAssembler extends DataAssembler 
{
	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		Object arg1, arg2;
		arg2 = objects.pop();
		arg1 = objects.pop();
		
		return new Pair<Object, Object>(arg1, arg2);
	}

}
