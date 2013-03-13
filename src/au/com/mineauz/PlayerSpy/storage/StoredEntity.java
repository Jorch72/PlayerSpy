package au.com.mineauz.PlayerSpy.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;

import org.bukkit.entity.*;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.wrappers.craftbukkit.CraftWorld;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.EntityShadowPlayer;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.EntityTypes;

public class StoredEntity 
{
	public StoredEntity()
	{
		
	}
	public StoredEntity(Entity ent)
	{
		mTypeId = ent.getType().getTypeId();
		if(mTypeId == -1)
			mTypeId = (short) (1024 | ent.getType().ordinal());
		
		mLocation = new StoredLocation(ent.getLocation());
		mOriginalId = ent.getEntityId();
		
		if(ent.getType() == EntityType.PLAYER)
		{
			mPlayerName = ((Player)ent).getName();
		}
	}

	public void write(DataOutputStream stream) throws IOException
	{
		// entity type
		stream.writeShort(mTypeId);
		// id
		stream.writeInt(mOriginalId);
		// location
		mLocation.writeLocation(stream,true);
		// name if player
		if(mTypeId == (EntityType.PLAYER.ordinal() | 1024))
			stream.writeUTF(mPlayerName);
	}
	
	public void read(DataInputStream stream) throws IOException, RecordFormatException
	{
		// type
		mTypeId = stream.readShort();
		// id
		mOriginalId = stream.readInt();
		// location
		mLocation = StoredLocation.readLocationFull(stream);
		
		try
		{
			// name if player
			if(mTypeId == (EntityType.PLAYER.ordinal() | 1024))
				mPlayerName = stream.readUTF();
		}
		catch(UTFDataFormatException e)
		{
			throw new RecordFormatException("Error reading UTF string. Malformed data.");
		}
	}

	public static StoredEntity readEntity(DataInputStream stream) throws IOException, RecordFormatException
	{
		StoredEntity ent = new StoredEntity();
		ent.read(stream);
		
		return ent;
	}
	public int getSize()
	{
 		return 6 + mLocation.getSize(true) + (mTypeId == (EntityType.PLAYER.ordinal() | 1024) ? Utility.getUTFLength(mPlayerName) : 0);
	}
	
	public au.com.mineauz.PlayerSpy.wrappers.minecraft.Entity createEntity()
	{
		au.com.mineauz.PlayerSpy.wrappers.minecraft.Entity ent;
		if(mTypeId == (EntityType.PLAYER.ordinal() | 1024))
		{
			ent = new EntityShadowPlayer(CraftWorld.castFrom(mLocation.getLocation().getWorld()).getHandle(), mPlayerName);
		}
		else if((mTypeId & 1024) == 0)
			ent = EntityTypes.createEntityByID(mTypeId, CraftWorld.castFrom(mLocation.getLocation().getWorld()).getHandle());
		else
			return null;
		
		Utility.setEntityPosition(ent, mLocation.getLocation());
		return ent;
	}

	public EntityType getEntityType()
	{
		if((mTypeId & 1024) != 0)
		{
			return EntityType.values()[mTypeId & 1023];
		}
		
		return EntityType.fromId(mTypeId);
	}
	
	public void setEntityType( EntityType type )
	{
		mTypeId = type.getTypeId();
		if(mTypeId == -1)
			mTypeId = (short) (1024 | type.ordinal());
	}
	
	public org.bukkit.Location getLocation()
	{
		return mLocation.getLocation();
	}
	public String getPlayerName()
	{
		return mPlayerName;
	}
	public int getEntityId()
	{
		return mOriginalId;
	}
	
	public String getName()
	{
		if(getEntityType() == EntityType.PLAYER)
			return getPlayerName();
		else
			return getEntityType().getName();
	}
	
	private short mTypeId;
	private StoredLocation mLocation;
	private String mPlayerName;
	private int mOriginalId;
	
	
	
	
}
