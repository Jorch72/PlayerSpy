package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.storage.StoredLocation;

public class TeleportRecord extends Record implements IPlayerLocationAware
{
	public TeleportRecord(Location whereTo, TeleportCause cause) 
	{
		super(RecordType.Teleport);
		mLocation = new StoredLocation(whereTo);
		mCause = (byte)cause.ordinal();
	}
	public TeleportRecord() 
	{
		super(RecordType.Teleport);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		mLocation.writeLocation(stream, true);
		stream.writeByte(mCause);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		mLocation = StoredLocation.readLocationFull(stream);
		mCause = stream.readByte();
	}

	public Location getLocation() 
	{
		return mLocation.getLocation();
	}
	public TeleportCause getCause()
	{
		return TeleportCause.values()[mCause];
	}
	private StoredLocation mLocation;
	private byte mCause;
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return mLocation.getSize(true) + 1;
	}
	
	@Override
	public boolean isFullLocation() { return true; }
	@Override
	public String getDescription()
	{
		if(getCause() == TeleportCause.COMMAND || getCause() == TeleportCause.PLUGIN )
			return "%s was teleported to " + Utility.locationToStringShort(mLocation.getLocation());
		return null;
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof TeleportRecord))
			return false;
		
		TeleportRecord record = (TeleportRecord)obj;
		
		return mCause == record.mCause && mLocation.equals(record.mLocation);
	}
	
	@Override
	public String toString()
	{
		return "TeleportRecord { cause: " + TeleportCause.values()[mCause] + " to: " + mLocation + " }";
	}
}
