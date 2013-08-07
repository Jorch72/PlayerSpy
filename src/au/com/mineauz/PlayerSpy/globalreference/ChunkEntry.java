package au.com.mineauz.PlayerSpy.globalreference;

import java.io.IOException;
import java.io.RandomAccessFile;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

public class ChunkEntry extends IndexEntry
{
	public static int getEntrySize()
	{
		return 28;
	}
	
	public int chunkX;
	public int chunkZ;
	public int worldHash;
	
	public long location = 0;
	public long size = 0;
	public long count = 0;
	
	public long padding = 0;
	
	public ChunkEntry()
	{
		
	}

	@Override
	public void read( RandomAccessFile file ) throws IOException
	{
		chunkX = file.readInt();
		chunkZ = file.readInt();
		worldHash = file.readInt();
		
		location = Utility.getUnsignedInt(file.readInt());
		size = Utility.getUnsignedInt(file.readInt());
		count = Utility.getUnsignedInt(file.readInt());
		
		padding = Utility.getUnsignedInt(file.readInt());
	}

	@Override
	public void write( RandomAccessFile file ) throws IOException
	{
		file.writeInt(chunkX);
		file.writeInt(chunkZ);
		file.writeInt(worldHash);
		
		file.writeInt((int)location);
		file.writeInt((int)size);
		file.writeInt((int)count);
		
		file.writeInt((int)padding);
	}
	
	@Override
	public long getLocation()
	{
		return location;
	}
	
	@Override
	public long getSize()
	{
		return size;
	}

	@Override
	public String toString()
	{
		return String.format("ChunkEntry: {x: %d z: %d world: %d loc: %X size: %X count: %d padding: %X}", chunkX, chunkZ, worldHash, location, size, count, padding);
	}
}
