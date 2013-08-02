package au.com.mineauz.PlayerSpy.globalreference;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.lang.Validate;

import au.com.mineauz.PlayerSpy.structurefile.Index;
import au.com.mineauz.PlayerSpy.structurefile.SpaceLocator;
import au.com.mineauz.PlayerSpy.structurefile.StructuredFile;

public class FileIndex extends Index<FileEntry>
{
	private GRFileHeader mHeader;
	private HashMap<String, Integer> mNameMap;
	private HashMap<UUID, Integer> mIdMap;
	
	public FileIndex( StructuredFile hostingFile, GRFileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(hostingFile, file, locator);
		mHeader = header;
		
		mNameMap = new HashMap<String, Integer>();
		mIdMap = new HashMap<UUID, Integer>(); 
	}

	@Override
	public String getIndexName()
	{
		return "File Index";
	}

	@Override
	public long getLocation()
	{
		return mHeader.FileIndexLocation;
	}

	@Override
	public long getSize()
	{
		return mHeader.FileIndexSize;
	}

	@Override
	protected int getEntrySize()
	{
		return FileEntry.getByteSize(mHeader.VersionMajor);
	}

	@Override
	protected FileEntry createNewEntry()
	{
		FileEntry entry = new FileEntry();
		entry.version = mHeader.VersionMajor;
		return entry;
	}

	@Override
	protected int getElementCount()
	{
		return mHeader.FileIndexCount;
	}

	@Override
	protected void updateElementCount( int newCount )
	{
		mHeader.FileIndexCount = newCount;

	}

	@Override
	protected void updateSize( long newSize )
	{
		mHeader.FileIndexSize = newSize;
	}

	@Override
	protected void updateLocation( long newLocation )
	{
		mHeader.FileIndexLocation = newLocation;
	}

	@Override
	protected void saveChanges() throws IOException
	{
		mFile.seek(0);
		mHeader.write(mFile);
	}
	
	private void rebuildMaps()
	{
		mNameMap = new HashMap<String, Integer>();
		mIdMap = new HashMap<UUID, Integer>();
		
		for(int index = 0; index < getCount(); ++index)
		{
			FileEntry entry = get(index);
			mNameMap.put(entry.fileName, index);
			mIdMap.put(entry.fileId, index);
		}
	}
	
	@Override
	public void read() throws IOException
	{
		super.read();
		
		rebuildMaps();
	}
	
	@Override
	public int add( FileEntry entry ) throws IOException
	{
		entry.version = mHeader.VersionMajor;
		int index = super.add(entry);
		
		rebuildMaps();
		
		return index;
	}
	
	public FileEntry get(String logName)
	{
		Integer result = mNameMap.get(logName);
		if(result != null)
			return get(result);
		return null;
	}
	
	public FileEntry get(UUID id)
	{
		Validate.notNull(id);
		Integer result = mIdMap.get(id);
		if(result != null)
			return get(result);
		return null;
	}
	
	public void set(UUID id, FileEntry entry) throws IOException
	{
		Validate.notNull(id);
		Validate.notNull(entry);
		Validate.isTrue(id == entry.fileId, "Cannot change the file id");
		
		
		Integer result = mIdMap.get(id);
		Validate.notNull(result, "No file with id " + id.toString() + " exists");
		
		set(result, entry);
	}
	
	public void remove(UUID id) throws IOException
	{
		FileEntry entry = get(id);
		if (entry == null)
			return;
		
		remove(entry);
	}

}
