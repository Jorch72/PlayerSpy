package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;

import au.com.mineauz.PlayerSpy.Utilities.BoundingBox;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

// Represents a session declaration
public class SessionEntry extends IndexEntry
{
	public static int getByteSize(int version)
	{
		switch(version)
		{
		case 1:
			return 27;
		case 2:
			return 35;
		case 3:
			return 39 + 2*(Utility.cBitSetSize/8);
		case 4:
			return 87;
		default:
			throw new IllegalArgumentException("Invalid version number " + version);
		}
	}
	public int version;
	
	public long StartTimestamp;
	public long EndTimestamp;
	public short RecordCount;
	public long Location;
	public long TotalSize;
	public long Padding;
	
	public BoundingBox playerBB;
	public BoundingBox otherBB;
	
	public boolean Compressed;
	public int Id;
	public int OwnerTagId = -1;
	
	public void write(RandomAccessFile file) throws IOException
	{
		if(version != FileHeader.currentVersion)
			throw new RuntimeException("Attempted to write an old version. Only the current version can be written");
		
		file.writeLong(StartTimestamp);
		file.writeLong(EndTimestamp);
		file.writeShort(RecordCount);
		file.writeInt((int)Location);
		file.writeInt((int)TotalSize);
		file.writeByte(Compressed == true ? 1 : 0);
		file.writeInt(Id);
		file.writeInt(OwnerTagId);
		
		file.writeInt((int)Padding);
		
		playerBB.write(file);
		otherBB.write(file);
	}
	public void read(RandomAccessFile file) throws IOException
	{
		StartTimestamp = file.readLong();
		EndTimestamp = file.readLong();
		RecordCount = file.readShort();
		Location = (long)file.readInt();
		TotalSize = (long)file.readInt();
		Compressed = (file.readByte() == 0 ? false : true);
		
		if(version >= 2)
		{
			Id = file.readInt();
			OwnerTagId = file.readInt();
		}
		
		if(version >= 3)
		{
			Padding = (long)file.readInt();
			if(version == 3)
			{
				byte[] bytes = new byte[Utility.cBitSetSize/8];
				// This data is obsolete, just read the bytes and discard it
				file.readFully(bytes);
				file.readFully(bytes);
			}
		}
		
		if(version >= 4)
		{
			playerBB = new BoundingBox();
			playerBB.read(file);
			otherBB = new BoundingBox();
			otherBB.read(file);
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
	public int hashCode()
	{
		return Id ^ OwnerTagId;
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
	
	@Override
	public String toString()
	{
		return "Session: " + Id;
	}
}