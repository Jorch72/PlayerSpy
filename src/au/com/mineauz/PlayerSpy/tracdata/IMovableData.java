package au.com.mineauz.PlayerSpy.tracdata;

public interface IMovableData<T extends IndexEntry> extends IData<T>
{
	public void setLocation(long newLocation);
}
