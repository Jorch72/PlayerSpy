package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.*;

public class LoginRecord extends Record implements ILocationAware
{
	public LoginRecord(Location loginLocaiton)
	{
		super(RecordType.Login);
		mLocation = new StoredLocation(loginLocaiton);
	}
	public LoginRecord()
	{
		super(RecordType.Login);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		mLocation.writeLocation(stream, true);
	}
	
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mLocation = StoredLocation.readLocationFull(stream);
	}

	public Location getLocation()
	{
		return mLocation.getLocation();
	}
	
	private StoredLocation mLocation;

	@Override
	protected int getContentSize(boolean absolute) 
	{
		return mLocation.getSize(true);
	}
	@Override
	public boolean isFullLocation() { return true; }
	
	@Override
	public String getDescription()
	{
		return "%s logged on";
	}
}
