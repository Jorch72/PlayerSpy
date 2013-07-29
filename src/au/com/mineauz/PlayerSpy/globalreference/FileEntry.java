package au.com.mineauz.PlayerSpy.globalreference;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.UUID;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

public class FileEntry extends IndexEntry
{
	public static int getByteSize()
	{
		return 32 + cMaxFileNameLength;
	}

	public static final int cMaxFileNameLength = 32;
	
	public UUID fileId;
	
	// Max size of filename is 32 chars, it should only have the logname and ext
	public String fileName;
	
	public long timeBegin;
	public long timeEnd;

	@Override
	public void read( RandomAccessFile file ) throws IOException
	{
		fileId = new UUID(file.readLong(), file.readLong());
		
		char[] nameData = new char[cMaxFileNameLength];
		for(int i = 0; i < cMaxFileNameLength; i++)
			nameData[i] = (char)file.readByte();
		
		fileName = String.valueOf(nameData);
		if(fileName.indexOf(0) != -1)
			fileName = fileName.substring(0, fileName.indexOf(0));
		
		byte[] bytes = new byte[Utility.cBitSetSize/8];
		file.readFully(bytes);
		
		timeBegin = file.readLong();
		timeEnd = file.readLong();
	}

	@Override
	public void write( RandomAccessFile file ) throws IOException
	{
		file.writeLong(fileId.getMostSignificantBits());
		file.writeLong(fileId.getLeastSignificantBits());
		
		byte[] nameData = new byte[cMaxFileNameLength];
		Arrays.fill(nameData, (byte)0);
		for(int i = 0; i < fileName.length() && i < cMaxFileNameLength; i++)
			nameData[i] = (byte)fileName.charAt(i);
		
		file.write(nameData);
		
		file.writeLong(timeBegin);
		file.writeLong(timeEnd);
		
	}

}
