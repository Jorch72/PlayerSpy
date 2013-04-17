package au.com.mineauz.PlayerSpy.globalreference;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.UUID;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

public class SessionEntry extends IndexEntry
{
	public static int getByteSize()
	{
		return 36 + 2*(Utility.cBitSetSize/8);
	}
	
	public int sessionId;
	public UUID fileId;
	public long startTime;
	public long endTime;
	public BitSet chunkFilter;
	public BitSet locationFilter;
	

	@Override
	public void read( RandomAccessFile file ) throws IOException
	{
		sessionId = file.readInt();
		
		fileId = new UUID(file.readLong(), file.readLong());
		
		startTime = file.readLong();
		endTime = file.readLong();
		
		byte[] bytes = new byte[Utility.cBitSetSize/8];
		file.readFully(bytes);
		chunkFilter = BitSet.valueOf(bytes);
		file.readFully(bytes);
		locationFilter = BitSet.valueOf(bytes);
	}

	@Override
	public void write( RandomAccessFile file ) throws IOException
	{
		file.writeInt(sessionId);
		
		file.writeLong(fileId.getMostSignificantBits());
		file.writeLong(fileId.getLeastSignificantBits());
		
		file.writeLong(startTime);
		file.writeLong(endTime);
		
		file.write(Utility.bitSetToBytes(chunkFilter));
		file.write(Utility.bitSetToBytes(locationFilter));
	}

}
