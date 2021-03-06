package au.com.mineauz.PlayerSpy.globalreference;

import java.io.IOException;
import java.io.RandomAccessFile;

import au.com.mineauz.PlayerSpy.structurefile.IData;
import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

public class GRFileHeader implements IData<IndexEntry>
{
	public static final byte currentVersion = 2;
	public static final byte currentVersionMinor = 0;
	public byte VersionMajor = currentVersion;
	public byte VersionMinor = currentVersionMinor;
	
	public long HolesIndexLocation;
	public long HolesIndexSize;
	public int HolesIndexCount;
	public short HolesIndexPadding;
	
	public long FileIndexLocation;
	public long FileIndexSize;
	public int FileIndexCount;
	
	public long SessionIndexLocation;
	public long SessionIndexSize;
	public int SessionIndexCount;
	
	public long ChunkIndexLocation;
	public long ChunkIndexSize;
	public int ChunkIndexCount;
	
	public GRFileHeader()
	{
	}

	@Override
	public long getLocation()
	{
		return 0;
	}

	@Override
	public long getSize()
	{
		return 42;
	}

	public void write(RandomAccessFile file) throws IOException
	{
		file.writeByte(VersionMajor);
		file.writeByte(VersionMinor);
		
		file.writeInt((int)HolesIndexLocation);
		file.writeInt((int)HolesIndexSize);
		file.writeShort((short)HolesIndexCount);
		
		file.writeInt((int)FileIndexLocation);
		file.writeInt((int)FileIndexSize);
		file.writeShort((short)FileIndexCount);
		
		file.writeInt((int)SessionIndexLocation);
		file.writeInt((int)SessionIndexSize);
		file.writeShort((short)SessionIndexCount);
		
		file.writeInt((int)ChunkIndexLocation);
		file.writeInt((int)ChunkIndexSize);
		file.writeShort((short)ChunkIndexCount);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		VersionMajor = file.readByte();
		VersionMinor = file.readByte();
		
		if(VersionMajor != 1 && VersionMajor != 2)
			throw new IOException("Unsupported file version " + VersionMajor + "." + VersionMinor + "!");
		
		HolesIndexLocation = (long) file.readInt();
		HolesIndexSize = (long)file.readInt();
		HolesIndexCount = (int)file.readShort();
		
		FileIndexLocation = (long) file.readInt();
		FileIndexSize = (long)file.readInt();
		FileIndexCount = (int)file.readShort();
		
		SessionIndexLocation = (long) file.readInt();
		SessionIndexSize = (long)file.readInt();
		SessionIndexCount = (int)file.readShort();
		
		ChunkIndexLocation = (long) file.readInt();
		ChunkIndexSize = (long)file.readInt();
		ChunkIndexCount = (int)file.readShort();
	}
	
	@Override
	public IndexEntry getIndexEntry()
	{
		return null;
	}

	@Override
	public String toString()
	{
		return "ReferenceFileHeader";
	}
}
