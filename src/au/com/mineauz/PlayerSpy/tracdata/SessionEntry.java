package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;

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
	
	public BitSet LocationFilter = new BitSet(Utility.cBitSetSize);
	public BitSet ChunkLocationFilter = new BitSet(Utility.cBitSetSize);
	
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
			file.write(Utility.bitSetToBytes(LocationFilter));
			file.write(Utility.bitSetToBytes(ChunkLocationFilter));
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
			byte[] bytes = new byte[Utility.cBitSetSize/8];
			file.readFully(bytes);
			LocationFilter = BitSet.valueOf(bytes);
			file.readFully(bytes);
			ChunkLocationFilter = BitSet.valueOf(bytes);
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
}