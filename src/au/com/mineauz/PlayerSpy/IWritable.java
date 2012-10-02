package au.com.mineauz.PlayerSpy;

import java.io.IOException;
import java.io.RandomAccessFile;

public interface IWritable 
{
	public void write(RandomAccessFile file) throws IOException;
	public void read(RandomAccessFile file) throws IOException;
	public int getSize();
}
