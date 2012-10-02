package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;

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
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		stream.writeByte(mLogoffType.ordinal());
		if(mLogoffType != LogoffType.Quit)
			stream.writeUTF(mReason);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
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
	protected int getContentSize() 
	{
		return 1 + (mLogoffType != LogoffType.Quit ? 2 + mReason.length() : 0);
	}
}
