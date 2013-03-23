package au.com.mineauz.PlayerSpy.Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import au.com.mineauz.PlayerSpy.debugging.Debug;


public class ACIDRandomAccessFile extends RandomAccessFile
{
	private Journal mJournal;
	ReentrantLock mJournalLock;
	
	private ACIDRandomAccessFile mMaster;
	private List<ACIDRandomAccessFile> mChildren = new ArrayList<ACIDRandomAccessFile>();
	
	public ACIDRandomAccessFile( File file, String mode ) throws FileNotFoundException
	{
		super(file, mode);
		
		mJournalLock = new ReentrantLock(true);
		try
		{
			mJournal = new Journal(file, this);
			
			if(mJournal.isHot())
			{
				Journal journal = mJournal;
				journal.findAndAttachChildren();
				
				while(journal.hasMaster())
				{
					Journal master = Journal.findAndCreateJournal(mJournal.getMasterFile());
					master.attach(journal);
					master.findAndAttachChildren();
					journal = master;
				}
				
				journal.rollback();
				
				seek(0);
				Debug.info("File %s had an open journal on load. Rollback was issued", file.getName());
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

	public void beginTransaction(ACIDRandomAccessFile master) throws IOException
	{
		if(master != null)
		{
			if (!master.mJournalLock.isHeldByCurrentThread())
				throw new IllegalStateException("Cannot begin joint transaction, this thread does not own the transaction.");
			
			if (!master.mJournal.isHot())
				throw new IllegalStateException("Cannot begin joint transaction, the master has not begun yet.");
		}
		
		try
		{
			if(!mJournalLock.tryLock(0, TimeUnit.SECONDS))
				throw new IllegalStateException("Cannot begin transaction, there is already one in progress.");
			
			if(mJournal.isHot())
				throw new IllegalStateException("Cannot begin transaction, there is already one in progress.");
		
			if(master == null)
				mJournal.begin(length());
			else
			{
				mJournal.begin(length(), master.mJournal);
				master.mChildren.add(this);
			}
			
			mMaster = master;
			mChildren.clear();
			
			Debug.finest("Begun transaction");
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}
	}
	public void beginTransaction() throws IOException
	{
		beginTransaction(null);
	}
	
	public void commit() throws IOException
	{
		if(mMaster != null && mMaster.mJournal.isHot())
			throw new IllegalStateException("Cannot commit this transaction as it is controlled by the master.");
		
		mJournalLock.unlock();
		
		// The journals handle the chain.
		if(mMaster == null)
			mJournal.clear();
		
		Debug.finest("Committed transaction");
		
		for(ACIDRandomAccessFile child : mChildren)
			child.commit();
		
		mChildren.clear();
		mMaster = null;
	}
	
	public void rollback()
	{
		if(mMaster != null && mMaster.mJournal.isHot())
			throw new IllegalStateException("Cannot rollback this transaction as it is controlled by the master.");
		
		mJournalLock.unlock();
		try
		{
			// The journals handle the chain.
			if(mMaster == null)
				mJournal.rollback();
			Debug.info("Transaction was rolled back");
			
			for(ACIDRandomAccessFile child : mChildren)
				child.rollback();
			
			mChildren.clear();
			mMaster = null;
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
			mJournal.preWrite(b.length);
			
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
			mJournal.preWrite(len);
					
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
			mJournal.preWrite(1);
					
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
