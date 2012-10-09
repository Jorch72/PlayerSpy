package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class ChatCommandActionDA extends DataAssembler
{
	private boolean mUseContains;
	public ChatCommandActionDA(boolean useContains)
	{
		mUseContains = useContains;
	}
	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		 ChatCommandAction action = new ChatCommandAction();
		 
		 if(mUseContains)
		 {
			 String containsStr = (String)objects.pop();
			 objects.pop();
			 action.contains = containsStr;
		 }
		 
		 String type = (String)objects.pop();
		 if(!type.equals("chat"))
			 action.command = true;
		 
		 return action;
	}

}
