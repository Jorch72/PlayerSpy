package au.com.mineauz.PlayerSpy;

import java.io.IOException;
import java.io.RandomAccessFile;

public class RollbackEntry
{
	public static int cSize = 12;
	
	public int sessionId;
	public long detailLocation;
	public long detailSize;
	
	public void write(RandomAccessFile output) throws IOException
	{
		output.writeInt(sessionId);
		output.writeInt((int)detailLocation);
		output.writeInt((int)detailSize);
	}
	
	public void read(RandomAccessFile input) throws IOException
	{
		sessionId = input.readInt();
		detailLocation = input.readInt();
		detailSize = input.readInt();
	}
}
