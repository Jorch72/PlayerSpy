package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;

import au.com.mineauz.PlayerSpy.Utility;

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
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mLogoffType = LogoffType.values()[stream.readByte()];
		if(mLogoffType != LogoffType.Quit)
			mReason = stream.readUTF();
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
			return "%s was banned for '" + mReason + "'";
		else if(mLogoffType == LogoffType.Kick)
			return "%s was kicked for '" + mReason + "'";
		else
			return "%s logged off";
	}
}
