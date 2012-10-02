package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.*;

public class DeathRecord extends Record implements ILocationAware
{

	public DeathRecord(Location deathLocation, String reason) 
	{
		super(RecordType.Death);
		mLocation = new StoredLocation(deathLocation);
		mReason = reason;
	}
	public DeathRecord() 
	{
		super(RecordType.Death);
	}

	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		mLocation.writeLocation(stream, false);
		stream.writeUTF(mReason);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mLocation = StoredLocation.readLocation(stream, currentWorld);
		mReason = stream.readUTF();
	}
	
	public Location getLocation()
	{
		return mLocation.getLocation();
	}
	public String getReason()
	{
		return mReason;
	}
	
	private StoredLocation mLocation;
	private String mReason;
	@Override
	protected int getContentSize() 
	{
		return mLocation.getSize(false) + 2 + mReason.length();
	}
	@Override
	public boolean isFullLocation() { return false;	}

}
