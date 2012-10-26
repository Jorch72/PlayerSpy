package au.com.mineauz.PlayerSpy.Records;

import java.io.*;
import java.util.Calendar;

import org.bukkit.World;

public abstract class Record 
{
	protected Record(RecordType type)
	{
		mType = type;
		mTimestamp = Calendar.getInstance().getTimeInMillis();
	}
	
	public boolean write(DataOutputStream stream, boolean absolute)
	{
		try
		{
			stream.writeByte((byte)mType.ordinal());
			stream.writeLong(mTimestamp);
			
			writeContents(stream, absolute);
			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
	public boolean read(DataInputStream stream, World currentWorld, boolean absolute)
	{
		try
		{
			// Dont read type since that was already read
			mTimestamp = stream.readLong();
			
			readContents(stream, currentWorld, absolute);
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	public long getTimestamp()
	{
		return mTimestamp;
	}
	public void setTimestamp( long timestamp )
	{
		mTimestamp = timestamp;
	}
	public RecordType getType()
	{
		return mType;
	}
	protected abstract void writeContents(DataOutputStream stream, boolean absolute) throws IOException;
	protected abstract void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException;
	
	public static Record readRecord(DataInputStream stream, World currentWorld, int version, boolean absolute)
	{
		try
		{
			Record record = RecordRegistry.makeRecord(version, stream.readByte());

			if(record != null && record.read(stream, currentWorld, absolute))
				return record;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	public int getSize(boolean absolute)
	{
		return 9 + getContentSize(absolute);
	}
	protected abstract int getContentSize(boolean absolute);
	
	public abstract String getDescription();
	
	private RecordType mType;
	private long mTimestamp;
	
}

