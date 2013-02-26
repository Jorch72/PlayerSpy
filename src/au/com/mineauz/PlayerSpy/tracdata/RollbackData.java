package au.com.mineauz.PlayerSpy.tracdata;

public class RollbackData implements IMovableData<RollbackEntry>
{
	private final RollbackEntry mRollbackEntry;
	
	public RollbackData(RollbackEntry entry)
	{
		mRollbackEntry = entry;
	}
	
	@Override
	public RollbackEntry getIndexEntry()
	{
		return mRollbackEntry;
	}
	
	public long getSize()
	{
		return mRollbackEntry.detailSize;
	}
	
	@Override
	public long getLocation()
	{
		return mRollbackEntry.detailLocation;
	}
	@Override
	public void setLocation( long newLocation )
	{
		mRollbackEntry.detailLocation = newLocation;
	}
}
