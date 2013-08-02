package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

public class ChunkEntry extends IndexEntry
{
	public static int getEntrySize()
	{
		return 14;
	}
	
	public int listId;
	public long location;
	public long size;
	public int count;
	
	
	public ChunkEntry()
	{
	}

	@Override
	public void read( RandomAccessFile file ) throws IOException
	{
		listId = file.readInt();
		location = Utility.getUnsignedInt(file.readInt());
		size = Utility.getUnsignedInt(file.readInt());
		count = file.readUnsignedShort();
	}

	@Override
	public void write( RandomAccessFile file ) throws IOException
	{
		file.writeInt(listId);
		file.writeInt((int)location);
		file.writeInt((int)size);
		file.writeShort((short)count);
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
}
