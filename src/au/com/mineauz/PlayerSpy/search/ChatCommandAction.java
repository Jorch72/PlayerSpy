package au.com.mineauz.PlayerSpy.search;

public class ChatCommandAction extends Action
{
	public boolean command;
	public String contains;
	
	@Override
	public String toString() 
	{
		return "{ isCommand: " + command + (contains != null ? ", must contain: '" + contains + "'" : "") + " }";
	}
}
