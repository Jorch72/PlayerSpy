package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.StoredLocation;

public class SleepRecord extends Record {

	public SleepRecord(boolean isSleeping, Location bedLocation) 
	{
		super(RecordType.Sleep);
		
	}
	public SleepRecord()
	{
		super(RecordType.Sleep);
	}

	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		stream.writeBoolean(mIsSleeping);
		mLocation.writeLocation(stream, false);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mIsSleeping = stream.readBoolean();
		mLocation = StoredLocation.readLocation(stream, currentWorld);
	}

	@Override
	protected int getContentSize() 
	{
		return 1 + mLocation.getSize(false);
	}

	public boolean isSleeping()
	{
		return mIsSleeping;
	}
	
	public Location getBedLocation()
	{
		return mLocation.getLocation();
	}
	
	private boolean mIsSleeping;
	private StoredLocation mLocation;
}
