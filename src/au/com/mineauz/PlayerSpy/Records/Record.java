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
	
	public boolean write(DataOutputStream stream)
	{
		try
		{
			stream.writeByte((byte)mType.ordinal());
			stream.writeLong(mTimestamp);
			
			writeContents(stream);
			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
	public boolean read(DataInputStream stream, World currentWorld)
	{
		try
		{
			// Dont read type since that was already read
			mTimestamp = stream.readLong();
			
			readContents(stream, currentWorld);
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}
	public long getTimestamp()
	{
		return mTimestamp;
	}
	public RecordType getType()
	{
		return mType;
	}
	protected abstract void writeContents(DataOutputStream stream) throws IOException;
	protected abstract void readContents(DataInputStream stream, World currentWorld) throws IOException;
	
	public static Record readRecord(DataInputStream stream, World currentWorld, int version)
	{
		try
		{
			Record record = RecordRegistry.makeRecord(version, stream.readByte());

			if(record != null && record.read(stream, currentWorld))
				return record;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	public int getSize()
	{
		return 9 + getContentSize();
	}
	protected abstract int getContentSize();
	
	private RecordType mType;
	private long mTimestamp;
}

