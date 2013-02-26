package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;

public class RollbackData implements IData<RollbackEntry>
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
	
	public short[] items = new short[0];
	public short padding = 8;
	
	public void write(RandomAccessFile output) throws IOException
	{
		output.writeShort(items.length);
		output.writeShort(padding);
		
		for(int i = 0; i < items.length; ++i)
			output.writeShort(items[i]);
		
		output.write(new byte[padding]);
	}
	
	public void read(RandomAccessFile input) throws IOException
	{
		int count = input.readShort();
		padding = input.readShort();
		
		items = new short[count];
		
		for(int i = 0; i < count; ++i)
			items[i] = input.readShort();
	}
	
	public long getSize()
	{
		return 4 + padding + items.length * 2;
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
