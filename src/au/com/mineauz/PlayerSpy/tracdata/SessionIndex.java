package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;

public class SessionIndex extends DataIndex<SessionEntry, SessionData>
{
	private static int NextId = (int)(System.currentTimeMillis() / 1000);
	private HashMap<Integer, Integer> mSessionMap = new HashMap<Integer, Integer>();
	private HashMap<String, Integer> mActiveSessions = new HashMap<String, Integer>();
	
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
		
		mActiveSessions.clear();
		
		// Find the active sessions
		for(int i = 0; i < mElements.size(); ++i)
		{
			SessionEntry session = get(i);
			if(session.Compressed)
				continue;
			
			String ownerTag = mLog.getOwnerTag(session);
			if(mActiveSessions.containsKey(ownerTag))
			{
				SessionEntry other = getSessionFromId(mActiveSessions.get(ownerTag));
				
				if(session.EndTimestamp > other.EndTimestamp)
					mActiveSessions.put(ownerTag, session.Id);
			}
			else
				mActiveSessions.put(ownerTag, session.Id);
		}
	}
	
	@Override
	public int add( SessionEntry entry ) throws IOException
	{
		entry.version = mHeader.VersionMajor;
		entry.Id = NextId++;
		
		int index = super.add(entry);
		
		rebuildSessionMap();
		
		return index;
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
	
	public SessionEntry getActiveSessionFor(String ownerTag)
	{
		if(mActiveSessions.containsKey(ownerTag))
			return getSessionFromId(mActiveSessions.get(ownerTag));
		
		return null;
	}
	
	public void setActiveSession(String ownerTag, SessionEntry session)
	{
		if(session == null)
			mActiveSessions.remove(ownerTag);
		else
			mActiveSessions.put(ownerTag, session.Id);
	}

	@Override
	protected SessionData createNewDataElement( SessionEntry entry )
	{
		return new SessionData(entry);
	}
}
