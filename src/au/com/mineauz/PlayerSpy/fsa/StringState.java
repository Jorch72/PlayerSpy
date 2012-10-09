package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

public class StringState extends State
{
	private String mMatch; 
	public StringState(String matchStr)
	{
		mMatch = matchStr;
	}
	@Override
	public boolean match(String word, ArrayDeque<Object> output) 
	{
		if(mMatch == null || word.equals(mMatch))
		{
			output.push(word);
			return true;
		}
		return false;
	}
	@Override
	public String getExpected() 
	{
		return (mMatch != null ? "'" + mMatch + "'" : "text");
	}

}
