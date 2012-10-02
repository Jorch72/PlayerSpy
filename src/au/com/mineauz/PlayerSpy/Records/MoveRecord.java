package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.*;

public class MoveRecord extends Record implements ILocationAware
{

	public MoveRecord(Location location, Location headLocation) 
	{
		super(RecordType.Move);
		mLocation = new StoredLocation(location);
		mHeadLocation = new StoredLocation(headLocation);
	}
	
	public MoveRecord() 
	{
		super(RecordType.Move);
	}

	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		mLocation.writeLocation(stream, false);
		// head yaw
		stream.writeByte((byte)(mHeadLocation.getLocation().getYaw() * 256.0F / 360.0F));
		// head pitch
		stream.writeByte((byte)(mHeadLocation.getLocation().getPitch() * 256.0F / 360.0F));
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mLocation = StoredLocation.readLocation(stream, currentWorld);
		byte yaw,pitch;
		yaw = stream.readByte();
		pitch = stream.readByte();
		
		Location head = mLocation.getLocation().clone();
		head.setYaw(yaw * 360F / 256F);
		head.setPitch(pitch * 360F / 256F);
		
		mHeadLocation = new StoredLocation(head);
	}

	public Location getLocation()
	{
		return mLocation.getLocation();
	}
	public Location getHeadLocation()
	{
		return mHeadLocation.getLocation();
	}
	private StoredLocation mLocation;
	private StoredLocation mHeadLocation;
	
	@Override
	protected int getContentSize() 
	{
		return 2 + mLocation.getSize(false);
	}
	
	@Override
	public boolean isFullLocation() { return false; }
}
