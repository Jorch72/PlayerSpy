package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Vehicle;

import au.com.mineauz.PlayerSpy.storage.StoredEntity;

public class VehicleMountRecord extends Record implements ILocationAware 
{

	public VehicleMountRecord(boolean isMounting, Vehicle mounted) 
	{
		super(RecordType.VehicleMount);
		
		mIsMounting = isMounting;
		mVehicle = new StoredEntity(mounted);
	}
	public VehicleMountRecord()
	{
		super(RecordType.VehicleMount);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeBoolean(mIsMounting);
		mVehicle.write(stream);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException 
	{
		mIsMounting = stream.readBoolean();
		mVehicle = StoredEntity.readEntity(stream);
	}

	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 1 + mVehicle.getSize();
	}

	public StoredEntity getVehicle()
	{
		return mVehicle;
	}
	
	public boolean isMounting()
	{
		return mIsMounting;
	}
	
	private StoredEntity mVehicle;
	private boolean mIsMounting;
	@Override
	public String getDescription()
	{
		String entityName = (mVehicle.getEntityType() == EntityType.PLAYER ? mVehicle.getPlayerName() : mVehicle.getEntityType().getName());
		if(mIsMounting)
			return "%s mounted " + ChatColor.DARK_AQUA + entityName + ChatColor.RESET;
		else
			return "%s dismounted " + ChatColor.DARK_AQUA + entityName + ChatColor.RESET;
	}
	@Override
	public Location getLocation()
	{
		return mVehicle.getLocation();
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof VehicleMountRecord))
			return false;
		
		VehicleMountRecord record = (VehicleMountRecord)obj;
		
		return mIsMounting == record.mIsMounting && mVehicle.equals(record.mVehicle);
	}
	
	@Override
	public String toString()
	{
		return "VehicleMountRecord { mounting: " + mIsMounting + " vehicle: " + mVehicle.toString() + " }";
	}
	
}
