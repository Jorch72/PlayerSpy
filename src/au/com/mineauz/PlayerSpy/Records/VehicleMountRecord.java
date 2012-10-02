package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.entity.Vehicle;

import au.com.mineauz.PlayerSpy.StoredEntity;

public class VehicleMountRecord extends Record {

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
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		stream.writeBoolean(mIsMounting);
		mVehicle.write(stream);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mIsMounting = stream.readBoolean();
		mVehicle = StoredEntity.readEntity(stream);
	}

	@Override
	protected int getContentSize() 
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
	
}
