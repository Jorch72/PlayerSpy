package au.com.mineauz.PlayerSpy.globalreference;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import au.com.mineauz.PlayerSpy.structurefile.Index;
import au.com.mineauz.PlayerSpy.structurefile.SpaceLocator;
import au.com.mineauz.PlayerSpy.structurefile.StructuredFile;

public class SessionIndex extends Index<SessionEntry>
{
	private GRFileHeader mHeader;
	
	public SessionIndex( StructuredFile hostingFile, GRFileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(hostingFile, file, locator);
		mHeader = header;
	}

	@Override
	public String getIndexName()
	{
		return "Session Index";
	}

	@Override
	public long getLocation()
	{
		return mHeader.SessionIndexLocation;
	}

	@Override
	public long getSize()
	{
		return mHeader.SessionIndexSize;
	}

	@Override
	protected int getEntrySize()
	{
		return SessionEntry.getByteSize(mHeader.VersionMajor);
	}

	@Override
	protected SessionEntry createNewEntry()
	{
		SessionEntry ent = new SessionEntry();
		ent.version = mHeader.VersionMajor;
		return ent;
	}

	@Override
	protected int getElementCount()
	{
		return mHeader.SessionIndexCount;
	}

	@Override
	protected void updateElementCount( int newCount )
	{
		mHeader.SessionIndexCount = newCount;
	}

	@Override
	protected void updateSize( long newSize )
	{
		mHeader.SessionIndexSize = newSize;
	}

	@Override
	protected void updateLocation( long newLocation )
	{
		mHeader.SessionIndexLocation = newLocation;
	}

	@Override
	protected void saveChanges() throws IOException
	{
		mFile.seek(0);
		mHeader.write(mFile);
	}

	@Override
	public int add( SessionEntry entry ) throws IOException
	{
		entry.version = mHeader.VersionMajor;
		return super.add(entry);
	}
	
	public SessionEntry get(UUID fileId, int sessionId)
	{
		int index = 0;
		for(SessionEntry session : this)
		{
			if(session.fileId.equals(fileId) && session.sessionId == sessionId)
				return get(index);
			++index;
		}
		
		return null;
	}
	
	public void remove(UUID fileId, int sessionId) throws IOException
	{
		int index = 0;
		for(SessionEntry session : this)
		{
			if(session.fileId.equals(fileId) && session.sessionId == sessionId)
			{
				remove(index);
				return;
			}
			++index;
		}
	}
	
	@Override
	protected void onRemove( SessionEntry entry )
	{
		entry.version = mHeader.VersionMajor;
	}
	
	public int getCount(UUID fileId)
	{
		int count = 0;
		for(SessionEntry session : this)
		{
			if(session.fileId.equals(fileId))
				++count;
		}
		
		return count;
	}
	
	public List<SessionEntry> subset(UUID fileId)
	{
		ArrayList<SessionEntry> list = new ArrayList<SessionEntry>();
		
		for(SessionEntry session : this)
		{
			if(session.fileId.equals(fileId))
				list.add(session);
		}
		
		return list;
	}
	
}
