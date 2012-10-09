package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

public class IntState extends State
{
	private int mMinValue;
	private int mMaxValue;
	
	public IntState()
	{
		mMaxValue = -1;
		mMinValue = 1;
	}
	public IntState(int minValue, int maxValue)
	{
		mMinValue = minValue;
		mMaxValue = maxValue;
	}
	
	@Override
	public boolean match(String word, ArrayDeque<Object> output) 
	{
		int val = 0;
		try
		{
			val = Integer.parseInt(word);
		}
		catch(NumberFormatException e)
		{
			return false;
		}
		
		if(mMinValue <= mMaxValue)
		{
			if(val < mMinValue || val > mMaxValue)
				return false;
		}
		
		output.push(val);
		return true;
	}
	@Override
	public String getExpected() 
	{
		if(mMinValue <= mMaxValue)
			return "integer between " + mMinValue + " and " + mMaxValue;

		return "integer";
		
	}

}
