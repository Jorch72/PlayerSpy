package au.com.mineauz.PlayerSpy.storage;

import java.io.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class StoredLocation 
{
	public StoredLocation(Location loc)
	{
		mLocation = loc.clone();
	}
	
	public Location getLocation()
	{
		return mLocation;
	}
	
	public void writeLocation(DataOutputStream stream, boolean full) throws IOException
	{
		// x
		stream.writeInt((int)(mLocation.getX() * 32D));
		// y
		stream.writeInt((int)(mLocation.getY() * 32D));
		// z
		stream.writeInt((int)(mLocation.getZ() * 32D));
		
		// yaw
		stream.writeByte((byte)(mLocation.getYaw() * 256.0F / 360.0F));
		// pitch
		stream.writeByte((byte)(mLocation.getPitch() * 256.0F / 360.0F));
		
		if(full)
		{
			// world
			stream.writeUTF(mLocation.getWorld().getName());
		}
	}
	
	public static StoredLocation readLocationFull(DataInputStream stream) throws IOException, RecordFormatException
	{
		int x,y,z;
		byte pitch,yaw;
		
		x = stream.readInt();
		y = stream.readInt();
		z = stream.readInt();
		
		yaw = stream.readByte();
		pitch = stream.readByte();
		
		try
		{
			Location loc;
			String world = stream.readUTF();
			
			loc = new Location(Bukkit.getWorld(world), x/32D,y/32D,z/32D,(float)yaw * 360F / 256F,pitch * 360F / 256F);
			if(loc.getWorld() == null)
			{
				loc.setWorld(Bukkit.getWorlds().get(0));
				//LogUtil.warning("Invalid world '" + world + "'in record. Defaulting to '" + loc.getWorld().getName() + "'. Did you delete a world?");
			}
			
			return new StoredLocation(loc);
		}
		catch(UTFDataFormatException e)
		{
			throw new RecordFormatException("Error reading UTF string. Malformed data.");
		}
		
		
	}
	public static StoredLocation readLocation(DataInputStream stream, World currentWorld) throws IOException
	{
		int x,y,z;
		byte pitch,yaw;
		
		x = stream.readInt();
		y = stream.readInt();
		z = stream.readInt();
		
		yaw = stream.readByte();
		pitch = stream.readByte();
		
		Location loc;
		loc = new Location(currentWorld, x/32D,y/32D,z/32D,(float)yaw * 360F / 256F,pitch * 360F / 256F);
		
		return new StoredLocation(loc);
	}
	public int getSize(boolean full)
	{
		return 14 + (full ? Utility.getUTFLength(mLocation.getWorld().getName()) : 0);
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof StoredLocation))
			return false;
		
		return mLocation.equals(((StoredLocation)obj).mLocation);
	}
	
	@Override
	public String toString()
	{
		return mLocation.toString();
	}
	
	private Location mLocation;
}
