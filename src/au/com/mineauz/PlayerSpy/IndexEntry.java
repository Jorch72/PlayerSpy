package au.com.mineauz.PlayerSpy;

import java.io.IOException;
import java.io.RandomAccessFile;

// Represents a session declaration
public class IndexEntry
{
	public static final int cSize = 27;
	
	public long StartTimestamp;
	public long EndTimestamp;
	public short RecordCount;
	public long Location;
	public long TotalSize;
	public boolean Compressed;
	
	
	public boolean write(RandomAccessFile file)
	{
		try
		{
			file.writeLong(StartTimestamp);
			file.writeLong(EndTimestamp);
			file.writeShort(RecordCount);
			file.writeInt((int)Location);
			file.writeInt((int)TotalSize);
			file.writeByte(Compressed == true ? 1 : 0);
			
			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
	public boolean read(RandomAccessFile file)
	{
		try
		{
			StartTimestamp = file.readLong();
			EndTimestamp = file.readLong();
			RecordCount = file.readShort();
			Location = (long)file.readInt();
			TotalSize = (long)file.readInt();
			Compressed = (file.readByte() == 0 ? false : true);

			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
}