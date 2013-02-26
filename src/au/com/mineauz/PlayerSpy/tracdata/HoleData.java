package au.com.mineauz.PlayerSpy.tracdata;

public class HoleData implements IMovableData<HoleEntry>
{
	private final HoleEntry mHole;
	
	public HoleData(HoleEntry hole)
	{
		mHole = hole;
	}
	
	@Override
	public HoleEntry getIndexEntry()
	{
		return mHole;
	}
	
	@Override
	public long getLocation()
	{
		return mHole.Location;
	}
	@Override
	public void setLocation( long newLocation )
	{
		mHole.Location = newLocation;
	}

	@Override
	public long getSize()
	{
		return mHole.Size;
	}

}
