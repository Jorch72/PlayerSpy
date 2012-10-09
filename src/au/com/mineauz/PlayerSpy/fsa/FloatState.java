package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

public class FloatState extends State
{
	private float mMinValue;
	private float mMaxValue;
	
	public FloatState()
	{
		mMaxValue = -1;
		mMinValue = 1;
	}
	public FloatState(float minValue, float maxValue)
	{
		mMinValue = minValue;
		mMaxValue = maxValue;
	}
	
	@Override
	public boolean match(String word, ArrayDeque<Object> output) 
	{
		float val = 0;
		try
		{
			val = Float.parseFloat(word);
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
			return "number between " + mMinValue + " and " + mMaxValue;
		else
			return "number";
			
	}

}
