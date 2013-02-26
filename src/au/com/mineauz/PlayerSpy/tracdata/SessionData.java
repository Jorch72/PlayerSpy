package au.com.mineauz.PlayerSpy.tracdata;

public class SessionData implements IMovableData<SessionEntry>
{
	private final SessionEntry mSession;
	
	public SessionData(SessionEntry session)
	{
		mSession = session;
	}
	
	@Override
	public SessionEntry getIndexEntry()
	{
		return mSession;
	}
	
	@Override
	public long getLocation()
	{
		return mSession.Location;
	}
	@Override
	public void setLocation( long newLocation )
	{
		mSession.Location = newLocation;
	}
	

	@Override
	public long getSize()
	{
		return mSession.TotalSize;
	}

}
