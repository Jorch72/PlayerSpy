package au.com.mineauz.PlayerSpy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import au.com.mineauz.PlayerSpy.Utilities.ACIDRandomAccessFile;

public class ACIDTest
{
	public void test1()
	{
		try
		{
			ACIDRandomAccessFile file = new ACIDRandomAccessFile(new File("acidtest.dat"), "rw");

			file.beginTransaction();
			file.seek(0);
			for(int i = 0; i < 100; i++)
			{
				byte c = (byte)((i % 10) +  (int)'0');
				
				file.writeByte(c);
			}
			
			file.commit();
			file.close();
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	public void test2()
	{
		try
		{
			ACIDRandomAccessFile file = new ACIDRandomAccessFile(new File("acidtest.dat"), "rw");

			file.beginTransaction();
			file.seek(0);
			for(int i = 0; i < 100; i++)
			{
				byte c = (byte)((i % 26) +  (int)'A');
				
				file.writeByte(c);
			}
			
			file.commit();
			file.close();
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
