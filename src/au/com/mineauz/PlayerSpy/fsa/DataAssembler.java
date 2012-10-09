package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

public abstract class DataAssembler extends NullState
{
	public abstract Object assemble(ArrayDeque<Object> objects);

	@Override
	public boolean match(String word, ArrayDeque<Object> output) 
	{
		output.push(assemble(output));
		return true;
	}
}
