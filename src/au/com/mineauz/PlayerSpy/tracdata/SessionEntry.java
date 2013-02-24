package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;

// Represents a session declaration
public class SessionEntry extends IndexEntry
{
	public static final int[] cSize = new int[] {0, 27, 35, 35};
	public int version;
	
	public long StartTimestamp;
	public long EndTimestamp;
	public short RecordCount;
	public long Location;
	public long TotalSize;
	public boolean Compressed;
	public int Id;
	public int OwnerTagId = -1;
	
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeLong(StartTimestamp);
		file.writeLong(EndTimestamp);
		file.writeShort(RecordCount);
		file.writeInt((int)Location);
		file.writeInt((int)TotalSize);
		file.writeByte(Compressed == true ? 1 : 0);
		if(version == 2 || version == 3)
		{
			file.writeInt(Id);
			file.writeInt(OwnerTagId);
		}
	}
	public void read(RandomAccessFile file) throws IOException
	{
		StartTimestamp = file.readLong();
		EndTimestamp = file.readLong();
		RecordCount = file.readShort();
		Location = (long)file.readInt();
		TotalSize = (long)file.readInt();
		Compressed = (file.readByte() == 0 ? false : true);
		if(version == 2 || version == 3)
		{
			Id = file.readInt();
			OwnerTagId = file.readInt();
		}
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof SessionEntry))
			return false;
		
		SessionEntry other = (SessionEntry)obj;
		
		if(Id == other.Id && OwnerTagId == other.OwnerTagId)
			return true;
		return false;
	}
	
	@Override
	public long getLocation()
	{
		return Location;
	}
	
	public long getSize()
	{
		return TotalSize;
	}
}