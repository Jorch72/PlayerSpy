package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

public class NullState extends State 
{

	@Override
	public boolean match(String word, ArrayDeque<Object> output) 
	{
		return true;
	}

	@Override
	public String getExpected() 
	{
		return null;
	}

}
