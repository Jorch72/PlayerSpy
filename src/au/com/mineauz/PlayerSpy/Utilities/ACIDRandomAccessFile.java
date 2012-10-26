package au.com.mineauz.PlayerSpy.Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import au.com.mineauz.PlayerSpy.LogUtil;


public class ACIDRandomAccessFile extends RandomAccessFile
{
	private Journal mJournal;
	ReentrantLock mJournalLock;
	
	public ACIDRandomAccessFile( File file, String mode ) throws FileNotFoundException
	{
		super(file, mode);
		
		mJournalLock = new ReentrantLock(true);
		try
		{
			mJournal = new Journal(file, this);
			
			if(mJournal.isHot())
			{
				mJournal.rollback();
				LogUtil.fine("rolledback transaction");
			}
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public ACIDRandomAccessFile( String filename, String mode ) throws FileNotFoundException
	{
		this(new File(filename), mode);
	}

	public void beginTransaction() throws IOException
	{
		try
		{
			if(!mJournalLock.tryLock(0, TimeUnit.SECONDS))
				throw new IllegalStateException("Cannot begin transaction, there is already one in progress.");
			
			if(mJournal.isHot())
				throw new IllegalStateException("Cannot begin transaction, there is already one in progress.");
		
			mJournal.begin(length());
			LogUtil.fine("Begun transaction");
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}
	}
	
	public void commit() throws IOException
	{
		mJournalLock.unlock();
		mJournal.clear();
		LogUtil.fine("Committed transaction");
	}
	
	public void rollback()
	{
		mJournalLock.unlock();
		try
		{
			mJournal.rollback();
			LogUtil.fine("rolledback transaction");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void close() throws IOException
	{
		if(mJournal.isHot())
			throw new IllegalStateException("There is still an active transaction. Please commit or rollback the transaction before closing the file.");
		
		super.close();
	}
	
	@Override
	public void write( byte[] b ) throws IOException
	{
		if(!mJournal.isHot())
			throw new IllegalStateException("There is no transaction in progress. Please start one with begin()");
		
		try
		{
			if(!mJournalLock.tryLock(0, TimeUnit.SECONDS))
				throw new IllegalStateException("This thread is not the current owner of this transaction!");
		
			// Update the journal
			long location = getFilePointer();
			if(location < length())
			{
				byte[] old = new byte[b.length];
				read(old);
				seek(location);
				
				mJournal.addBytes(location, old);
			}
			
			super.write(b);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
		finally
		{
			mJournalLock.unlock();
		}
	}
	@Override
	public void write( byte[] b, int off, int len ) throws IOException
	{
		if(!mJournal.isHot())
			throw new IllegalStateException("There is no transaction in progress. Please start one with begin()");
		
		try
		{
			if(!mJournalLock.tryLock(0, TimeUnit.SECONDS))
				throw new IllegalStateException("This thread is not the current owner of this transaction!");
		
			// Update the journal
			long location = getFilePointer();
			if(location < length())
			{
				byte[] old = new byte[len];
				read(old);
				seek(location);
				
				mJournal.addBytes(location, old);
			}
					
			super.write(b, off, len);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
		finally
		{
			mJournalLock.unlock();
		}
	}
	@Override
	public void write( int b ) throws IOException
	{
		if(!mJournal.isHot())
			throw new IllegalStateException("There is no transaction in progress. Please start one with begin()");
		
		try
		{
			if(!mJournalLock.tryLock(0, TimeUnit.SECONDS))
				throw new IllegalStateException("This thread is not the current owner of this transaction!");
		
			// Update the journal
			long location = getFilePointer();
			if(location < length())
			{
				byte old = (byte)read();
				seek(location);
				
				mJournal.addByte(location, old);
			}
					
			super.write(b);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
		finally
		{
			mJournalLock.unlock();
		}
	}
	
	@Override
	public int read() throws IOException
	{
		return super.read();
	}
	
	@Override
	public int read( byte[] b ) throws IOException
	{
		return super.read(b);
	}
	@Override
	public int read( byte[] b, int off, int len ) throws IOException
	{
		return super.read(b, off, len);
	}
	
	
}
