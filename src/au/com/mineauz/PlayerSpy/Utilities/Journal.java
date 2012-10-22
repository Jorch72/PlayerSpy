package au.com.mineauz.PlayerSpy.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Journal
{
	/**
	 * File header contains original file size and filename of master journal
	 * 
	 * Immediatly following that we have a header which contains the location in the original file and the size of the following data
	 * Also we store the last location in memory
	 * as we add bytes, if they follow the last location, we just append them and update the last header
	 * If the location does not follow the last location, we add a new header
	 * and we keep going from there.
	 * 
	 * Then rolling back is easy, we just start at the beginning of the file, then follow it through. We load each header, seek to that location in the file, 
	 * then copy the number of bytes specified by that header over to the file.
	 * Then we delete this file 
	 */
	
	private RandomAccessFile mJournalStream;
	private RandomAccessFile mOwnerStream;
	private File mJournalPath;
	
	private long mLastLocation;
	private long mLastSectionHeaderLocation;
	private SectionHeader mCurrentSection;
	
	
	/**
	 * Creates a journal for the file specified
	 */
	public Journal(File filePath, RandomAccessFile file) throws IOException
	{
		mJournalPath = new File(filePath.getAbsolutePath() + ".journal");
		mOwnerStream = file;
		
		mLastLocation = 0;
		mLastSectionHeaderLocation = 0;
		
		if(mJournalPath.exists())
			mJournalStream = new RandomAccessFile(mJournalPath, "rw");
	}
	
	public boolean isHot()
	{
		return (mJournalStream != null);
	}
	
	public void addByte(long location, byte value) throws IOException
	{
		addBytes(location, new byte[] {value});
	}
	public void addBytes(long location, byte[] values) throws IOException
	{
		if(mJournalStream == null)
			throw new IllegalStateException("Cannot add to journal, journal is cold.");
		
		if(location != mLastLocation + 1)
		{
			// Start new section header
			SectionHeader header = new SectionHeader();
			header.originalLocation = location;
			header.totalSize = 0;
			
			mCurrentSection = header;
			mLastSectionHeaderLocation = mJournalStream.getFilePointer();
			header.write(mJournalStream);
		}
		
		// Append data onto the section
		mJournalStream.write(values);
		mCurrentSection.totalSize += values.length;

		// Update the section header
		long temp = mJournalStream.getFilePointer();
		mJournalStream.seek(mLastSectionHeaderLocation);
		mCurrentSection.write(mJournalStream);
		mJournalStream.seek(temp);
		
		mLastLocation = location + values.length-1;
	}
	
	public void rollback() throws IOException
	{
		if(mJournalStream == null)
			throw new IllegalStateException("Cannot rollback journal, journal is cold.");
		
		mJournalStream.seek(0);
		
		JournalHeader mainHeader = new JournalHeader();
		mainHeader.read(mJournalStream);
		
		// First, restore the original file to its old file size
		mOwnerStream.setLength(mainHeader.originalFileSize);
		
		while(mJournalStream.getFilePointer() < mJournalStream.length())
		{
			// Rollback a section
			SectionHeader header = new SectionHeader();
			header.read(mJournalStream);
			
			mOwnerStream.seek(header.originalLocation);
			
			// Roll it back in 1kb chunks
			byte[] data = new byte[1024];
			int rcount = 0;
			for(long offset = 0; offset < header.totalSize; offset += 1024L)
			{
				if(header.totalSize - offset < 1024L)
				{
					// There is not enough data to fill the buffer
					rcount = (int)(header.totalSize - offset);
					mJournalStream.read(data, 0, rcount);
				}
				else
				{
					rcount = 1024;
					mJournalStream.read(data);
				}
				
				mOwnerStream.write(data,0,rcount);
			}
		}
		
		// Now delete the journal
		mJournalStream.close();
		mJournalStream = null;
		mJournalPath.delete();
	}
	
	public void clear() throws IOException
	{
		if(mJournalStream == null)
			throw new IllegalStateException("Cannot clear journal, journal is cold.");
		
		try
		{
			mJournalStream.close();
		}
		finally
		{
			mJournalStream = null;
			mJournalPath.delete();
		}
	}
	
	public void begin(long fileSize) throws IOException
	{
		if(mJournalStream != null)
			throw new IllegalStateException("Cannot begin journal, journal is hot.");
		
		try
		{
			mJournalStream = new RandomAccessFile(mJournalPath, "rw");
			
			JournalHeader mainHeader = new JournalHeader();
			mainHeader.masterJournal = "";
			mainHeader.originalFileSize = fileSize;
			
			mainHeader.write(mJournalStream);
		}
		catch(IOException e)
		{
			mJournalStream = null;
			throw e;
		}
	}
}

class JournalHeader
{
	public long originalFileSize;
	public String masterJournal;
	
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeLong(originalFileSize);
		file.writeUTF(masterJournal);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		originalFileSize = file.readLong();
		masterJournal = file.readUTF();
	}
}

class SectionHeader
{
	public long originalLocation;
	public long totalSize;
	
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeLong(originalLocation);
		file.writeLong(totalSize);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		originalLocation = file.readLong();
		totalSize = file.readLong();
	}
}