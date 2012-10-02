package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.*;

public class RespawnRecord extends Record implements ILocationAware
{
	public RespawnRecord(Location respawnLocation) 
	{
		super(RecordType.Respawn);
		mLocation = new StoredLocation(respawnLocation);
	}
	public RespawnRecord() 
	{
		super(RecordType.Respawn);
	}

	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		mLocation.writeLocation(stream,true);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mLocation = StoredLocation.readLocationFull(stream);
	}

	public Location getLocation()
	{
		return mLocation.getLocation();
	}
	StoredLocation mLocation;
	
	@Override
	protected int getContentSize() 
	{
		return mLocation.getSize(true);
	}
	
	@Override
	public boolean isFullLocation() { return true; }
}