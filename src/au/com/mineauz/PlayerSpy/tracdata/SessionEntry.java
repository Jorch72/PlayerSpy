package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;

import au.com.mineauz.PlayerSpy.Utilities.BloomFilter;
import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

// Represents a session declaration
public class SessionEntry extends IndexEntry
{
	public static final int[] cSize = new int[] {0, 27, 35, 51};
	public int version;
	
	public long StartTimestamp;
	public long EndTimestamp;
	public short RecordCount;
	public long Location;
	public long TotalSize;
	public long Padding;
	
	public BloomFilter LocationFilter = new BloomFilter();
	public BloomFilter ChunkLocationFilter = new BloomFilter();
	
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
		
		if(version == 3)
		{
			file.writeInt((int)Padding);
			file.writeLong(LocationFilter.getValue());
			file.writeLong(ChunkLocationFilter.getValue());
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
		
		if(version == 3)
		{
			Padding = (long)file.readInt();
			LocationFilter = new BloomFilter(file.readLong());
			ChunkLocationFilter = new BloomFilter(file.readLong());
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