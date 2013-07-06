package au.com.mineauz.PlayerSpy.Records;

import java.util.ArrayList;
import java.util.List;

public class RecordFormatException extends Exception
{
	private static final long	serialVersionUID	= 8496840474189456137L;

	private RecordType mType;
	private List<Record> mHistory;
	
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
	
	public void setSourceType(RecordType type)
	{
		mType = type;
	}
	
	public RecordType getSourceType()
	{
		return mType;
	}
	
	public void setHistory(List<Record> history)
	{
		// Keep 5 entries only
		if(history.isEmpty())
			return;
		
		mHistory = new ArrayList<Record>(5);
		for(int i = history.size()-1 ; i >= history.size()-5 && i >= 0; --i)
			mHistory.add(history.get(i));
	}
	
	@Override
	public String toString()
	{
		String output = super.toString();
		
		if(mType != null)
			output += " while attempting " + mType.toString();
		
		if(mHistory != null)
			output += "\nHistory: " + mHistory.toString();

		return output;
	}
	
}
