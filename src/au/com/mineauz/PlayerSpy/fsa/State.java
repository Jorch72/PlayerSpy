package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public abstract class State 
{
	private ArrayList<State> mNextStates = new ArrayList<State>();
	
	public State addNext(State nextState)
	{
		mNextStates.add(nextState);
		return this;
	}
	public List<State> getNextStates()
	{
		return mNextStates;
	}
	
	public State()
	{
	}
	
	public abstract boolean match(String word, ArrayDeque<Object> output);
	
	public abstract String getExpected();
}
