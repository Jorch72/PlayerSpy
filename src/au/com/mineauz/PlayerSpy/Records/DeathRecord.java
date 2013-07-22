package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;

import org.bukkit.Location;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.storage.StoredLocation;

public class DeathRecord extends Record implements IPlayerLocationAware
{

	public DeathRecord(Location deathLocation, String reason) 
	{
		super(RecordType.Death);
		mLocation = new StoredLocation(deathLocation);
		mReason = (reason == null ? "" : reason);
	}
	public DeathRecord() 
	{
		super(RecordType.Death);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		mLocation.writeLocation(stream, absolute);
		stream.writeUTF(mReason);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		try
		{
			if(absolute)
				mLocation = StoredLocation.readLocationFull(stream);
			else
				mLocation = StoredLocation.readLocation(stream, currentWorld);
			mReason = stream.readUTF();
		}
		catch(UTFDataFormatException e)
		{
			throw new RecordFormatException("Error reading UTF string. Malformed data.");
		}
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
	protected int getContentSize(boolean absolute) 
	{
		return mLocation.getSize(absolute) + Utility.getUTFLength(mReason);
	}
	@Override
	public boolean isFullLocation() { return false;	}
	@Override
	public String getDescription()
	{
		return "%s Died '" + mReason + "'";
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof DeathRecord))
			return false;
		
		DeathRecord record = (DeathRecord)obj;
		
		return (mLocation.equals(record.mLocation) && mReason.equals(record.mReason));
		
	}

	@Override
	public String toString()
	{
		return "Death {reason: " + mReason + " loc: " + mLocation + "}";
	}
}
