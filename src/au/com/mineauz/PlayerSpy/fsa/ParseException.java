package au.com.mineauz.PlayerSpy.fsa;

public class ParseException extends Exception
{
	private static final long serialVersionUID = -144555084858432987L;

	public ParseException()
	{
	}
	public ParseException(String message)
	{
		super(message);
	}
	
	public ParseException(Throwable throwable)
	{
		super(throwable);
	}
}
