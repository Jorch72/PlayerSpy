package au.com.mineauz.PlayerSpy.Records;

import java.io.*;
import java.util.Calendar;

import org.bukkit.World;

import au.com.mineauz.PlayerSpy.IndexEntry;
import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.debugging.Debug;

public abstract class Record 
{
	public LogFile sourceFile;
	public IndexEntry sourceEntry;
	public short sourceIndex;
	
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
			Debug.logException(e);
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
			Debug.logException(e);
			return false;
		}
		catch (RecordFormatException e)
		{
			Debug.info("Encoutered invalid record format for type %s. %s", mType.toString(), e.getMessage());
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
	protected abstract void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException;
	
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
			Debug.logException(e);
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

