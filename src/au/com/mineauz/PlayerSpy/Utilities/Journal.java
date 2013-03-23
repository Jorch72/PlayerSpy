package au.com.mineauz.PlayerSpy.Utilities;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import au.com.mineauz.PlayerSpy.debugging.Debug;

public class Journal
{
	private RandomAccessFile mJournalStream;
	private RandomAccessFile mOwnerStream;
	private File mOwnerPath;
	
	private File mJournalPath;
	private int mRandomVal;
	
	private Journal mMaster;
	private String mMasterPath;
	
	private List<Journal> mChildren = new ArrayList<Journal>();
	
	private HashSet<Integer> mWrittenSectors;
	
	private static HashMap<String, Journal> mActiveJournals = new HashMap<String, Journal>();
	
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
		{
			mJournalStream = new RandomAccessFile(mJournalPath, "rw");
			JournalHeader header = new JournalHeader();
			header.read(mJournalStream);
			mJournalStream.seek(0);
			
			if(header.masterJournal.isEmpty())
				mMasterPath = null;
			else
				mMasterPath = header.masterJournal;
			
			mActiveJournals.put(mJournalPath.getAbsolutePath(), this);
		}
	}
	
	/**
	 * Creates a quick journal used for rolling back chains of journals
	 */
	private Journal(File filePath) throws IOException
	{
		mJournalPath = new File(filePath.getAbsolutePath() + ".journal");
		mOwnerPath = filePath;
		
		mWrittenSectors = new HashSet<Integer>();
		mIsRollingBack = false;
		
		if(mJournalPath.exists())
		{
			mJournalStream = new RandomAccessFile(mJournalPath, "rw");
			JournalHeader header = new JournalHeader();
			header.read(mJournalStream);
			mJournalStream.seek(0);
			
			if(header.masterJournal.isEmpty())
				mMasterPath = null;
			else
				mMasterPath = header.masterJournal;
			
			mActiveJournals.put(mJournalPath.getAbsolutePath(), this);
		}
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
	public boolean hasMaster()
	{
		return mMasterPath != null;
	}
	
	public File getJournalFile()
	{
		return mJournalPath;
	}
	
	public int getRandomKey()
	{
		return mRandomVal;
	}
	
	public File getMasterFile()
	{
		if(!hasMaster())
			return null;
		
		return new File(mMasterPath);
	}
	
	void attach(Journal other)
	{
		mChildren.add(other);
		other.mMaster = this;
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
		
		if(mMaster != null && mMaster.isHot())
			throw new IllegalStateException("Cannot rollback journal, master journal is still hot.");
		
		RandomAccessFile stream = mOwnerStream;
		if(mOwnerStream == null)
			stream = new RandomAccessFile(mOwnerPath, "rw");
		
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
				Debug.severe("Found a corrupt journal entry (size fail). Ignoring");
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
				Debug.severe("Found a corrupt journal entry (checksum fail). Ignoring");
				continue;
			}
			
			long location = header.sectorNumber * (long)cSectorSize;
			
			stream.seek(location);
			
			stream.write(data);
		}
		// Restore the original file to its old file size
		stream.setLength(mainHeader.originalFileSize);
				
		if(mOwnerStream == null)
			stream.close();
		
		mIsRollingBack = false;
		
		// Now delete the journal
		mJournalStream.close();
		mJournalStream = null;
		mJournalPath.delete();
		mWrittenSectors = null;
		
		mActiveJournals.remove(mJournalPath.getAbsolutePath());
		
		for(Journal child : mChildren)
			child.rollback();
		
		mChildren.clear();
		mMaster = null;
		mMasterPath = null;
	}
	
	public void clear() throws IOException
	{
		if(!isHot())
			throw new IllegalStateException("Cannot clear journal, journal is cold.");
		
		if(mMaster != null && mMaster.isHot())
			throw new IllegalStateException("Cannot clear journal, master journal is still hot.");
		
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
		
		mActiveJournals.remove(mJournalPath.getAbsolutePath());
		
		for(Journal child : mChildren)
			child.clear();
		
		mChildren.clear();
		mMaster = null;
		mMasterPath = null;
	}
	
	public void begin(long fileSize, Journal master) throws IOException
	{
		if(mJournalStream != null)
			throw new IllegalStateException("Cannot begin journal, journal is hot.");
		
		if(mOwnerStream == null)
			throw new IllegalStateException("You cannot begin this journal as this is a quick journal used for restores on load.");
		
		if(master != null && !master.isHot())
			throw new IllegalStateException("Cannot join other journal. the master journal is cold.");
		
		try
		{
			mJournalStream = new RandomAccessFile(mJournalPath, "rw");
			mMaster = master;
			
			JournalHeader mainHeader = new JournalHeader();
			if(master != null)
			{
				mMaster.attach(this);
				mainHeader.masterJournal = master.getJournalFile().getAbsolutePath();
				mRandomVal = mainHeader.randomVal = master.getRandomKey();
				mMasterPath = mMaster.getJournalFile().getAbsolutePath();
			}
			else
			{
				mainHeader.masterJournal = "";
				mRandomVal = mainHeader.randomVal = new Random().nextInt();
				mMasterPath = null;
			}
			
			
			
			mainHeader.originalFileSize = fileSize;
			
			mainHeader.write(mJournalStream);
			
			mWrittenSectors = new HashSet<Integer>();
			mActiveJournals.put(mJournalPath.getAbsolutePath(), this);
		}
		catch(IOException e)
		{
			mJournalStream = null;
			throw e;
		}
	}
	
	public void begin(long fileSize) throws IOException
	{
		begin(fileSize, null);
	}
	
	public static Journal findAndCreateJournal(File journalFile) throws IOException
	{
		String path = journalFile.getAbsolutePath();
		Journal journal = null;
		
		// Search active journals first
		journal = mActiveJournals.get(path);
		if(journal != null)
			return journal;
		
		// Check if it exists at all
		if(!journalFile.exists())
			return null;
		
		// Create a quick journal
		String hotfilePath = path.substring(0, path.length() - (".journal").length());
		journal = new Journal(new File(hotfilePath));
		
		return journal;
	}
	
	private static JournalHeader scrapeHeader(File journalFile) throws IOException
	{
		RandomAccessFile file = new RandomAccessFile(journalFile, "rw");
		JournalHeader header = new JournalHeader();
		header.read(file);
		file.close();
		
		return header;
	}

	public void findAndAttachChildren() throws IOException
	{
		for (File journal : mJournalPath.getParentFile().listFiles(new JournalFileFilter()))
		{
			JournalHeader header = scrapeHeader(journal);
			
			if(header.masterJournal.equals(mJournalPath.getAbsolutePath()))
			{
				boolean found = false;
				for(Journal child : mChildren)
				{
					if(child.getJournalFile().equals(journal))
					{
						found = true;
						break;
					}
				}
				
				if(found)
					continue;
				
				// Load up a quick journal
				String hotfilePath = journal.getAbsolutePath().substring(0, journal.getAbsolutePath().length() - (".journal").length());
				Journal quickJournal = new Journal(new File(hotfilePath));
				
				attach(quickJournal);
			}
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

class JournalFileFilter implements FilenameFilter
{
	
	@Override
	public boolean accept( File dir, String name )
	{
		return name.endsWith(".journal");
	}
	
}