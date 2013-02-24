package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;

public class SessionIndex extends Index<SessionEntry>
{
	private static int NextId = (int)(System.currentTimeMillis() / 1000);
	private HashMap<Integer, Integer> mSessionMap = new HashMap<Integer, Integer>();
	
	public SessionIndex( LogFile log, FileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(log, header, file, locator);
	}

	@Override
	public String getIndexName()
	{
		return "Session Index";
	}

	@Override
	public long getLocation()
	{
		return mHeader.IndexLocation;
	}

	@Override
	public long getSize()
	{
		return mHeader.IndexSize;
	}

	@Override
	protected int getEntrySize()
	{
		return SessionEntry.cSize[mHeader.VersionMajor];
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
		return mHeader.SessionCount;
	}

	@Override
	protected void updateElementCount( int newCount )
	{
		mHeader.SessionCount = newCount;
	}

	@Override
	protected void updateSize( long newSize )
	{
		mHeader.IndexSize = newSize;
	}

	@Override
	protected void updateLocation( long newLocation )
	{
		mHeader.IndexLocation = newLocation;
	}

	private void rebuildSessionMap()
	{
		mSessionMap.clear();
		
		int index = 0;
		for(SessionEntry entry : mElements)
		{
			mSessionMap.put(entry.Id, index);
			++index;
		}
	}
	
	@Override
	public void read() throws IOException
	{
		super.read();
		
		rebuildSessionMap();
	}
	
	@Override
	public void add( SessionEntry entry ) throws IOException
	{
		entry.version = mHeader.VersionMajor;
		entry.Id = NextId++;
		
		super.add(entry);
		
		rebuildSessionMap();
	}
	
	@Override
	protected void onRemove( SessionEntry entry )
	{
		entry.version = mHeader.VersionMajor;
		CrossReferenceIndex.instance.removeSession(mLog, entry);
	}
	
	@Override
	public void remove( int index ) throws IOException
	{
		super.remove(index);
		
		rebuildSessionMap();
	}
	
	@Override
	protected int getInsertIndex( SessionEntry entry )
	{
		int insertIndex = 0;
		for(insertIndex = 0; insertIndex < mElements.size(); insertIndex++)
		{
			if(mElements.get(insertIndex).StartTimestamp > entry.StartTimestamp)
				break;
		}
		return insertIndex;
	}
	
	public SessionEntry getSessionFromId(int id)
	{
		if(mSessionMap.containsKey(id))
			return get(mSessionMap.get(id));
		
		return null;
	}
}
