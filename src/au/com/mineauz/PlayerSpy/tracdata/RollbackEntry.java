package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;

import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

public class RollbackEntry extends IndexEntry
{
	public static int cSize = 18;
	
	public int sessionId;
	public long detailLocation;
	public long detailSize;
	public long padding;
	public short count;
	
	public void write(RandomAccessFile output) throws IOException
	{
		output.writeInt(sessionId);
		output.writeInt((int)detailLocation);
		output.writeInt((int)detailSize);
		output.writeInt((int)padding);
		output.writeShort(count);
	}
	
	public void read(RandomAccessFile input) throws IOException
	{
		sessionId = input.readInt();
		detailLocation = input.readInt();
		detailSize = input.readInt();
		padding = input.readInt();
		count = input.readShort();
	}
	
	@Override
	public long getLocation()
	{
		return detailLocation;
	}
	
	public long getSize()
	{
		return detailSize;
	}
}
