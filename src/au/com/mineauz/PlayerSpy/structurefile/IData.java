package au.com.mineauz.PlayerSpy.structurefile;

public interface IData<T extends IndexEntry>
{
	public long getLocation();
	public long getSize();
	
	public T getIndexEntry();
}
