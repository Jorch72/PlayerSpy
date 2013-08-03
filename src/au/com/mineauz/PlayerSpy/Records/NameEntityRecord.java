package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.storage.StoredEntity;

public class NameEntityRecord extends Record implements ILocationAware
{
	private StoredEntity mEntity;
	private String mNewName;
	
	public NameEntityRecord()
	{
		super(RecordType.NameEntity);
	}
	
	public NameEntityRecord(LivingEntity namedEntity, String newName)
	{
		super(RecordType.NameEntity);
		
		mEntity = new StoredEntity(namedEntity);
		mNewName = newName;
	}

	public String getNewName()
	{
		return mNewName;
	}
	
	public StoredEntity getEntity()
	{
		return mEntity;
	}
	
	@Override
	protected void writeContents( DataOutputStream stream, boolean absolute ) throws IOException
	{
		mEntity.write(stream);
		stream.writeUTF(mNewName);
	}

	@Override
	protected void readContents( DataInputStream stream, World currentWorld, boolean absolute ) throws IOException, RecordFormatException
	{
		mEntity = StoredEntity.readEntity(stream);
		try
		{
			mNewName = stream.readUTF();
		}
		catch(UTFDataFormatException e)
		{
			throw new RecordFormatException(e);
		}
	}

	@Override
	protected int getContentSize( boolean absolute )
	{
		return mEntity.getSize() + Utility.getUTFLength(mNewName);
	}

	@Override
	public String getDescription()
	{
		return "%s renamed " + ChatColor.DARK_AQUA + mEntity.getName() + ChatColor.RESET + " to " + ChatColor.DARK_AQUA + mNewName;
	}

	@Override
	public Location getLocation()
	{
		return mEntity.getLocation();
	}

}
