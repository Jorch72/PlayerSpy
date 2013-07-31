package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;

import org.bukkit.World;

import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class LogoffRecord extends Record
{
	public enum LogoffType
	{
		Quit,
		Kick,
		Ban
	}
	public LogoffRecord(LogoffType type, String reason) 
	{
		super(RecordType.Logoff);
		mLogoffType = type;
		mReason = reason;
	}
	public LogoffRecord() 
	{
		super(RecordType.Logoff);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeByte(mLogoffType.ordinal());
		if(mLogoffType != LogoffType.Quit)
			stream.writeUTF(mReason);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException 
	{
		try
		{
			int id = stream.readByte();
			
			if(id < 0 || id > LogoffType.values().length)
				throw new RecordFormatException(String.format("Tried to use value %d for logoff type", id));
	
			mLogoffType = LogoffType.values()[id];
			if(mLogoffType != LogoffType.Quit)
				mReason = stream.readUTF();
		}
		catch(UTFDataFormatException e)
		{
			throw new RecordFormatException("Error reading UTF string. Malformed data.");
		}
	}
	
	public LogoffType getLogoffType()
	{
		return mLogoffType;
	}
	public String getReason()
	{
		return mReason;
	}
	private LogoffType mLogoffType;
	private String mReason;
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 1 + (mLogoffType != LogoffType.Quit ? Utility.getUTFLength(mReason) : 0);
	}
	@Override
	public String getDescription()
	{
		if(mLogoffType == LogoffType.Ban)
			return "%s was banned for '" + mReason.replaceAll("%", "%%") + "'";
		else if(mLogoffType == LogoffType.Kick)
			return "%s was kicked for '" + mReason.replaceAll("%", "%%") + "'";
		else
			return "%s logged off";
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof LogoffRecord))
			return false;
		
		LogoffRecord record = (LogoffRecord)obj;
		
		return (mLogoffType == record.mLogoffType && (mLogoffType == LogoffType.Quit || mReason.equals(record.mReason)));
	}
}
