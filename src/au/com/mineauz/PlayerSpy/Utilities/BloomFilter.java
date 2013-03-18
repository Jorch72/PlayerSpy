package au.com.mineauz.PlayerSpy.Utilities;

public class BloomFilter
{
	private int mFilter = 0;

	public BloomFilter()
	{
		
	}
	
	public BloomFilter(int filter)
	{
		mFilter = filter;
	}
	
	@Override
	public int hashCode()
	{
		return mFilter;
	}
	
	public void add(int hash)
	{
		mFilter |= hash;
	}
	
	public void add(BloomFilter other)
	{
		mFilter |= other.hashCode();
	}
	
	public void clear()
	{
		mFilter = 0;
	}
	
	public boolean isPresent(int hash)
	{
		return ((mFilter & hash) == hash);
	}
	
	@Override
	public String toString()
	{
		return String.format("%X", mFilter);
	}
}
