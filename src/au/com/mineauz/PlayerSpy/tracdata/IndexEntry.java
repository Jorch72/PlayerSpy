package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class IndexEntry
{
	public abstract void read(RandomAccessFile file) throws IOException;
	
	public abstract void write(RandomAccessFile file) throws IOException;
}
