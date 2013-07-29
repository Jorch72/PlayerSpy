package au.com.mineauz.PlayerSpy.globalreference;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import au.com.mineauz.PlayerSpy.Utilities.BoundingBox;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

public class SessionEntry extends IndexEntry
{
	public static int getByteSize(int version)
	{
		if(version == 1)
			return 36 + 2*(Utility.cBitSetSize/8);
		else if(version == 2)
			return 84;
		else
			throw new IllegalArgumentException();
	}
	
	public int version;
	
	public int sessionId;
	public UUID fileId;
	public long startTime;
	public long endTime;
	public BoundingBox playerBB;
	public BoundingBox otherBB;
	
	@Override
	public void read( RandomAccessFile file ) throws IOException
	{
		sessionId = file.readInt();
		
		fileId = new UUID(file.readLong(), file.readLong());
		
		startTime = file.readLong();
		endTime = file.readLong();
		
		if(version == 1)
		{
			// Deprecated data. Just consume it
			byte[] bytes = new byte[Utility.cBitSetSize/8];
			file.readFully(bytes);
			file.readFully(bytes);
		}
		else if(version >= 2)
		{
			playerBB = new BoundingBox();
			playerBB.read(file);
			otherBB = new BoundingBox();
			otherBB.read(file);
		}
	}

	@Override
	public void write( RandomAccessFile file ) throws IOException
	{
		if(version != GRFileHeader.currentVersion)
			throw new RuntimeException("Attempted to write an old version. Only the current version can be written");
		
		file.writeInt(sessionId);
		
		file.writeLong(fileId.getMostSignificantBits());
		file.writeLong(fileId.getLeastSignificantBits());
		
		file.writeLong(startTime);
		file.writeLong(endTime);
		
		playerBB.write(file);
		otherBB.write(file);
	}

}
