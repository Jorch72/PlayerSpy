package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

public class MultiStringState extends State 
{
	private String[] mMatchTargets;
	
	public MultiStringState(String... targets)
	{
		mMatchTargets = targets;
	}
	@Override
	public boolean match(String word, ArrayDeque<Object> output) 
	{
		for(String target : mMatchTargets)
		{
			if(word.equals(target))
			{
				output.push(mMatchTargets[0]);
				return true;
			}
		}
		return false;
	}

	@Override
	public String getExpected() 
	{
		return "'" + mMatchTargets[0] + "'";
	}

}
