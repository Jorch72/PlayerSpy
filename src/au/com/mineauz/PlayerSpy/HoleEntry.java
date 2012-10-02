package au.com.mineauz.PlayerSpy;

import java.io.IOException;
import java.io.RandomAccessFile;

// Represents an empty 
public class HoleEntry
{
	public static final int cSize = 8;
	
	// The absolute location of the hole
	public long Location;
	// The size of the hole
	public long Size;
	
	public IndexEntry AttachedTo;
	
	public boolean write(RandomAccessFile file)
	{
		try
		{
			file.writeInt((int)Location);
			file.writeInt((int)Size);
			
			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
	
	public boolean read(RandomAccessFile file)
	{
		try
		{
			Location = (long)file.readInt();
			Size = (long)file.readInt();
			
			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
}