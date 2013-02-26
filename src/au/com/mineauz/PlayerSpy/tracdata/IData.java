package au.com.mineauz.PlayerSpy.tracdata;

public interface IData<T extends IndexEntry>
{
	public long getLocation();
	public long getSize();
	
	public T getIndexEntry();
}
