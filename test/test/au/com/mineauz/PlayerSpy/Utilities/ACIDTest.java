package test.au.com.mineauz.PlayerSpy.Utilities;

import static org.junit.Assert.*;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import au.com.mineauz.PlayerSpy.Utilities.ACIDRandomAccessFile;

public class ACIDTest
{
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test(expected = IllegalStateException.class)
	public void writeNoTransaction() throws IOException
	{
		File acidFile = folder.newFile();
		ACIDRandomAccessFile testFile = new ACIDRandomAccessFile(acidFile, "rw");
		
		// Should throw IllegalStateException because we are not in a transaction
		testFile.write(new byte[] {1,2,3,4,5});
		
		testFile.close();
	}
	
	@Test
	public void successfullTransaction() throws IOException
	{
		File acidFile = folder.newFile();
		ACIDRandomAccessFile testFile = new ACIDRandomAccessFile(acidFile, "rw");
		
		testFile.beginTransaction();
		
		byte[] written = new byte[] {1,2,3,4,5,6,7,8,9,10};
		
		testFile.write(written);
		
		testFile.commit();
		
		// Now read it back
		testFile.seek(0);
		byte[] bytes = new byte[10];
		testFile.readFully(bytes);
		
		assertArrayEquals(written, bytes);
		
		testFile.close();
	}
	
	@Test(expected = EOFException.class)
	public void rolledBackTransaction() throws IOException
	{
		File acidFile = folder.newFile();
		ACIDRandomAccessFile testFile = new ACIDRandomAccessFile(acidFile, "rw");
		
		testFile.beginTransaction();
		
		byte[] written = new byte[] {1,2,3,4,5,6,7,8,9,10};
		
		testFile.write(written);
		
		testFile.rollback();
		
		// Now read it back
		testFile.seek(0);
		byte[] bytes = new byte[10];
		
		// Should get eof exception
		testFile.readFully(bytes);
		
		testFile.close();
	}
	
	@Test(expected = IllegalStateException.class)
	public void closeWithTransaction() throws IOException
	{
		File acidFile = folder.newFile();
		ACIDRandomAccessFile testFile = new ACIDRandomAccessFile(acidFile, "rw");
		
		testFile.beginTransaction();
		
		byte[] written = new byte[] {1,2,3,4,5,6,7,8,9,10};
		
		testFile.write(written);
		
		// Should get IllegalStateException
		testFile.close();
	}
	
	@SuppressWarnings( "resource" )
	@Test(expected=EOFException.class)
	public void hangingTransactionCleanup() throws IOException
	{
		File acidFile = folder.newFile();
		ACIDRandomAccessFile testFile = new ACIDRandomAccessFile(acidFile, "rw");
		
		testFile.beginTransaction();
		
		byte[] written = new byte[] {1,2,3,4,5,6,7,8,9,10};
		
		testFile.write(written);
		
		// Lost the reference, transaction still open
		testFile = null;
		
		
		// Transaction should have rolled back
		testFile = new ACIDRandomAccessFile(acidFile, "rw");
		
		// Now read it back
		testFile.seek(0);
		byte[] bytes = new byte[10];
		
		// Should throw EOFException
		testFile.readFully(bytes);

		testFile.close();
	}
	
	@Test
	public void jointNormal() throws IOException
	{
		File masterFile = folder.newFile();
		File childFile = folder.newFile();
		
		ACIDRandomAccessFile master = new ACIDRandomAccessFile(masterFile, "rw");
		ACIDRandomAccessFile child = new ACIDRandomAccessFile(childFile, "rw");
		
		master.beginTransaction();
		child.beginTransaction(master);
		
		byte[] written = new byte[] {1,2,3,4,5,6,7,8,9,10};
		
		master.write(written);
		child.write(written);
		
		master.commit();
		
		// Now read it back
		master.seek(0);
		child.seek(0);
		
		byte[] bytes = new byte[10];
		master.readFully(bytes);
		assertArrayEquals(written, bytes);
		
		child.readFully(bytes);
		assertArrayEquals(written, bytes);
		
		master.close();
		child.close();
	}
	
	@Test
	public void jointRollback() throws IOException
	{
		File masterFile = folder.newFile();
		File childFile = folder.newFile();
		
		ACIDRandomAccessFile master = new ACIDRandomAccessFile(masterFile, "rw");
		ACIDRandomAccessFile child = new ACIDRandomAccessFile(childFile, "rw");
		
		master.beginTransaction();
		child.beginTransaction(master);
		
		byte[] written = new byte[] {1,2,3,4,5,6,7,8,9,10};
		
		master.write(written);
		child.write(written);
		
		master.rollback();
		
		// Now read it back
		master.seek(0);
		child.seek(0);
		
		byte[] bytes = new byte[10];
		
		try
		{
			master.readFully(bytes);
			fail("master read should have failed");
		}
		catch(EOFException e)
		{
			// ok
		}
		
		try
		{
			child.readFully(bytes);
			fail("child read should have failed");
		}
		catch(EOFException e)
		{
			// ok
		}
		
		master.close();
		child.close();
	}
	
	@Test(expected = IllegalStateException.class)
	public void jointWrongCommit() throws IOException
	{
		File masterFile = folder.newFile();
		File childFile = folder.newFile();
		
		ACIDRandomAccessFile master = new ACIDRandomAccessFile(masterFile, "rw");
		ACIDRandomAccessFile child = new ACIDRandomAccessFile(childFile, "rw");
		
		master.beginTransaction();
		child.beginTransaction(master);
		
		byte[] written = new byte[] {1,2,3,4,5,6,7,8,9,10};
		
		master.write(written);
		child.write(written);
		
		// This is the wrong one. It should fail
		child.commit();
		
		master.close();
		child.close();
	}
	
	@Test(expected = IllegalStateException.class)
	public void jointWrongRollback() throws IOException
	{
		File masterFile = folder.newFile();
		File childFile = folder.newFile();
		
		ACIDRandomAccessFile master = new ACIDRandomAccessFile(masterFile, "rw");
		ACIDRandomAccessFile child = new ACIDRandomAccessFile(childFile, "rw");
		
		master.beginTransaction();
		child.beginTransaction(master);
		
		byte[] written = new byte[] {1,2,3,4,5,6,7,8,9,10};
		
		master.write(written);
		child.write(written);
		
		// This is the wrong one. It should fail
		child.rollback();
		
		master.close();
		child.close();
	}
	
	@SuppressWarnings( "resource" )
	@Test
	public void jointHangingTransaction() throws IOException
	{
		File masterFile = folder.newFile();
		File childFile = folder.newFile();
		
		ACIDRandomAccessFile master = new ACIDRandomAccessFile(masterFile, "rw");
		ACIDRandomAccessFile child = new ACIDRandomAccessFile(childFile, "rw");
		
		master.beginTransaction();
		child.beginTransaction(master);
		
		byte[] written = new byte[] {1,2,3,4,5,6,7,8,9,10};
		
		master.write(written);
		child.write(written);
		
		// lose the reference to them both. Transaction still open
		master = null;
		child = null;
		
		// Reaquire them both, transaction should rollback
		master = new ACIDRandomAccessFile(masterFile, "rw");
		child = new ACIDRandomAccessFile(childFile, "rw");
		
		// Now read it back
		master.seek(0);
		child.seek(0);
		
		byte[] bytes = new byte[10];
		
		try
		{
			master.readFully(bytes);
			fail("master read should have failed");
		}
		catch(EOFException e)
		{
			// ok
		}
		
		try
		{
			child.readFully(bytes);
			fail("child read should have failed");
		}
		catch(EOFException e)
		{
			// ok
		}
		
		master.close();
		child.close();
	}
	
	@Test(expected = IllegalStateException.class)
	public void jointWrongOrder() throws IOException
	{
		File masterFile = folder.newFile();
		File childFile = folder.newFile();
		
		ACIDRandomAccessFile master = new ACIDRandomAccessFile(masterFile, "rw");
		ACIDRandomAccessFile child = new ACIDRandomAccessFile(childFile, "rw");
		
		// Wrong order, Should throw IllegalStateException
		child.beginTransaction(master);
		master.beginTransaction();
		
		master.commit();
		
		master.close();
		child.close();
	}
}
