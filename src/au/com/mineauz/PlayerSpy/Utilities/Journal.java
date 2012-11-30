package au.com.mineauz.PlayerSpy.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Random;

import au.com.mineauz.PlayerSpy.LogUtil;

public class Journal
{
	private RandomAccessFile mJournalStream;
	private RandomAccessFile mOwnerStream;
	private File mJournalPath;
	private int mRandomVal;
	
	private HashSet<Integer> mWrittenSectors;
	
	public static final int cSectorSize = 4096;
	
	private boolean mIsRollingBack;
	/**
	 * Creates a journal for the file specified
	 */
	public Journal(File filePath, RandomAccessFile file) throws IOException
	{
		mJournalPath = new File(filePath.getAbsolutePath() + ".journal");
		mOwnerStream = file;
		
		mWrittenSectors = new HashSet<Integer>();
		mIsRollingBack = false;
		
		if(mJournalPath.exists())
			mJournalStream = new RandomAccessFile(mJournalPath, "rw");
	}
	
	public boolean isHot()
	{
		try 
		{
			return (mJournalStream != null && mJournalStream.length() > 22);
		} 
		catch (IOException e) 
		{
			return false;
		}
	}
	
	public void preWrite(long size) throws IOException
	{
		if(mIsRollingBack)
			return;
		
		if(mJournalStream == null)
			throw new IllegalStateException("Cannot add to journal, journal is cold.");
		
		int startSector = (int)(mOwnerStream.getFilePointer() / cSectorSize);
		int endSector = (int)((mOwnerStream.getFilePointer() + size) / cSectorSize);
		
		long currentLocation = mOwnerStream.getFilePointer();
		
		for(int sector = startSector; sector <= endSector; sector++)
		{
			if(!mWrittenSectors.contains(sector))
				saveSector(sector);
		}
		
		mOwnerStream.seek(currentLocation);
	}
	
	private void saveSector(int sector) throws IOException
	{
		byte[] sectorData = new byte[cSectorSize];
		
		mOwnerStream.seek(sector * (long)cSectorSize);
		mOwnerStream.read(sectorData);
		
		int checksum = mRandomVal;
		for(int i = cSectorSize - 200; i >= 0; i -= 200)
		{
			int val = Array.getInt(sectorData, i);
			checksum += val;
		}
		
		SectionHeader header = new SectionHeader();
		header.checksum = checksum;
		header.sectorNumber = sector;
		
		header.write(mJournalStream);
		mJournalStream.write(sectorData);
		
		mWrittenSectors.add(header.sectorNumber);
		
		// Update the header
		long oldLoc = mJournalStream.getFilePointer();
		mJournalStream.seek(JournalHeader.idBytes.length);
		mJournalStream.writeInt(mWrittenSectors.size());
		mJournalStream.seek(oldLoc);
	}
	
	public void rollback() throws IOException
	{
		if(!isHot())
			throw new IllegalStateException("Cannot rollback journal, journal is cold.");
		
		mJournalStream.seek(0);
		mIsRollingBack = true;
		
		JournalHeader mainHeader = new JournalHeader();
		mainHeader.read(mJournalStream);
		
		for(int i = 0; i < mainHeader.sectionCount; i++)
		{
			// Rollback a section
			SectionHeader header = new SectionHeader();
			header.read(mJournalStream);
			
			byte[] data = new byte[cSectorSize];
			// Every correct section will have a full sector worth of data
			if(mJournalStream.read(data) != cSectorSize)
			{
				LogUtil.warning("Found a corrupt journal entry. Ignoring");
				break;
			}

			// Check the checksum
			int checksum = mainHeader.randomVal;
			for(int x = cSectorSize - 200; x >= 0; x -= 200)
			{
				int val = Array.getInt(data, x);
				checksum += val;
			}
			
			if(header.checksum != checksum)
			{
				LogUtil.warning("Found a corrupt journal entry (checksum fail). Ignoring");
				continue;
			}
			
			long location = header.sectorNumber * (long)cSectorSize;
			
			mOwnerStream.seek(location);
			
			mOwnerStream.write(data);
		}
		// Restore the original file to its old file size
		mOwnerStream.setLength(mainHeader.originalFileSize);
				
		mIsRollingBack = false;
		
		// Now delete the journal
		mJournalStream.close();
		mJournalStream = null;
		mJournalPath.delete();
		mWrittenSectors = null;
	}
	
	public void clear() throws IOException
	{
		if(!isHot())
			throw new IllegalStateException("Cannot clear journal, journal is cold.");
		
		try
		{
			mJournalStream.close();
			mWrittenSectors = null;
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
			mRandomVal = mainHeader.randomVal = new Random().nextInt();
			mainHeader.originalFileSize = fileSize;
			
			mainHeader.write(mJournalStream);
			
			mWrittenSectors = new HashSet<Integer>();
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
	public static byte[] idBytes = new byte[] {'J','N','L',10,13,32};
	
	public int sectionCount;
	public int randomVal;
	public long originalFileSize;
	public String masterJournal;
	
	
	public void write(RandomAccessFile file) throws IOException
	{
		file.write(idBytes);
		file.writeInt(sectionCount);
		file.writeInt(randomVal);
		file.writeLong(originalFileSize);
		file.writeUTF(masterJournal);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		byte[] temp = new byte[idBytes.length];
		file.readFully(temp);
		for(int i = 0; i < temp.length; i++)
		{
			if(idBytes[i] != temp[i])
				throw new IOException("Not a journal file");
		}
		sectionCount = file.readInt();
		randomVal = file.readInt();
		originalFileSize = file.readLong();
		masterJournal = file.readUTF();
	}
}

class SectionHeader
{
	public int sectorNumber;
	public int checksum;
	
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeInt(sectorNumber);
		file.writeInt(checksum);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		sectorNumber = file.readInt();
		checksum = file.readInt();
	}
}