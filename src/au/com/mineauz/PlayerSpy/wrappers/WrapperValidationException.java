package au.com.mineauz.PlayerSpy.wrappers;

public class WrapperValidationException extends RuntimeException
{
	private static final long	serialVersionUID	= 4939919995110775230L;

	public WrapperValidationException()
	{
		
	}
	
	public WrapperValidationException(String message)
	{
		super(message);
	}
	
	public WrapperValidationException(Throwable cause)
	{
		super(cause);
	}
	
	@Override
	public String toString()
	{
		if(getMessage() != null)
			return "WrapperValidationException: " + getMessage();
		else
			return "WrapperValidationException";
	}
}
