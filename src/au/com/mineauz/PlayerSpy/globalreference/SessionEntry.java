package au.com.mineauz.PlayerSpy.globalreference;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import au.com.mineauz.PlayerSpy.Utilities.BloomFilter;
import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

public class SessionEntry extends IndexEntry
{
	public static final int cSize = 44;
	
	public int sessionId;
	public UUID fileId;
	public long startTime;
	public long endTime;
	public BloomFilter chunkFilter;
	public BloomFilter locationFilter;
	

	@Override
	public void read( RandomAccessFile file ) throws IOException
	{
		sessionId = file.readInt();
		
		fileId = new UUID(file.readLong(), file.readLong());
		
		startTime = file.readLong();
		endTime = file.readLong();
		
		chunkFilter = new BloomFilter(file.readInt());
		locationFilter = new BloomFilter(file.readInt());
	}

	@Override
	public void write( RandomAccessFile file ) throws IOException
	{
		file.writeInt(sessionId);
		
		file.writeLong(fileId.getMostSignificantBits());
		file.writeLong(fileId.getLeastSignificantBits());
		
		file.writeLong(startTime);
		file.writeLong(endTime);
		
		file.writeInt(chunkFilter.hashCode());
		file.writeInt(locationFilter.hashCode());

	}

}
