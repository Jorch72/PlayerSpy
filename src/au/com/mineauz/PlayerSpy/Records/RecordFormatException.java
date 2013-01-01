package au.com.mineauz.PlayerSpy.Records;

public class RecordFormatException extends Exception
{
	private static final long	serialVersionUID	= 8496840474189456137L;

	public RecordFormatException()
	{
		super();
	}
	
	public RecordFormatException(String message)
	{
		super(message);
	}
	
	public RecordFormatException(Throwable cause)
	{
		super(cause);
	}
}
