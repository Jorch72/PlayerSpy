package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

public class FiniteStateAutomata 
{
	public static ArrayDeque<Object> parse(String input, State against) throws ParseException
	{
		if(!(against instanceof InitialState))
			throw new IllegalArgumentException("against must be a start state");
		
		ArrayDeque<Object> output = new ArrayDeque<Object>();
		
		String[] words = input.split("\\s+");
		if(words.length == 0)
			words = new String[] { input };

		if(parseWord(words[0], words, 0, against, output))
			return output;
		
		return null;
	}
	
	private static boolean parseWord(String word, String[] words, int index, State state, ArrayDeque<Object> output) throws ParseException
	{
		if(!state.match(word, output))
			return false;

		if(state instanceof FinalState && index < words.length) // Hit the final state but havent finished parsing input
			throw new ParseException("Expected nothing after " + words[index-1]);
		else if(state instanceof FinalState)
			return true;
		
		// Dont increment if this is the initial state since it doesnt match anything
		if(!(state instanceof NullState))
		{
			index++;
		}
		String nextWord = (index >= words.length ? "" : words[index]);
		
		// Try to parse the rest
		for(State next : state.getNextStates())
		{
			ArrayDeque<Object> temp = output.clone();
			if(parseWord(nextWord, words, index, next, temp))
			{
				// Set the normal output to the temp one
				output.clear();
				output.addAll(temp);
				return true;
			}
		}
		
		// Could not parse a next state, error in input
		String exceptString = "Expected ";
		boolean first = true;
		for(State next : state.getNextStates())
		{
			if(next.getExpected() != null)
			{
				if(!first)
					exceptString += " or ";
				
				exceptString += next.getExpected();
				first = false;
			}
		}
		
		if(state instanceof InitialState)
			exceptString += " after beginning";
		else
			exceptString += " after " + word;
		
		throw new ParseException(exceptString);
	}
}
