package au.com.mineauz.PlayerSpy.Utilities;

public class BloomFilter
{
	private long mFilter = 0;

	public BloomFilter()
	{
		
	}
	
	public BloomFilter(long filter)
	{
		mFilter = filter;
	}
	
	public BloomFilter(BloomFilter other)
	{
		mFilter = other.mFilter;
	}
	
	@Override
	public int hashCode()
	{
		return (int)(mFilter ^ (mFilter >>> 32));
	}
	
	public void add(long hash)
	{
		mFilter |= hash;
	}
	
	public void add(BloomFilter other)
	{
		mFilter |= other.getValue();
	}
	
	public void clear()
	{
		mFilter = 0;
	}
	
	public boolean isPresent(long hash)
	{
		return ((mFilter & hash) == hash);
	}
	
	public long getValue()
	{
		return mFilter;
	}
	
	@Override
	public String toString()
	{
		return String.format("%X", mFilter);
	}
}
