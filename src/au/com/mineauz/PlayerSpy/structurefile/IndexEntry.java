package au.com.mineauz.PlayerSpy.structurefile;

import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class IndexEntry
{
	public abstract void read(RandomAccessFile file) throws IOException;
	
	public abstract void write(RandomAccessFile file) throws IOException;
	
	/**
	 * Gets the location of any data this entry points to
	 */
	public long getLocation()
	{
		return 0;
	}
	
	/**
	 * Gets the size of any data this entry points to
	 */
	public long getSize()
	{
		return 0;
	}
}
