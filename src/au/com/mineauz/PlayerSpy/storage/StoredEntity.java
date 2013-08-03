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
		EntityType type = ent.getType();
		
		mTypeId = type.getTypeId();
		if(mTypeId == -1)
			mTypeId = (short) (1024 | ent.getType().ordinal());
		
		mLocation = new StoredLocation(ent.getLocation());
		mOriginalId = ent.getEntityId();
		
		if(ent.getType() == EntityType.PLAYER)
		{
			mCustomName = ((Player)ent).getName();
		}
		else if(ent instanceof LivingEntity)
		{
			mCustomName = ((LivingEntity)ent).getCustomName();
			if(mCustomName == null)
				mCustomName = "";
			
			if(!mCustomName.isEmpty())
				mTypeId |= 2048;
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
			stream.writeUTF(mCustomName);
		else if (getEntityType().isAlive() && (mTypeId & 2048) == 2048)
			stream.writeUTF(mCustomName);
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
			if(mTypeId - (mTypeId & 2048) == (EntityType.PLAYER.ordinal() | 1024))
				mCustomName = stream.readUTF();
			else if(getEntityType().isAlive() && (mTypeId & 2048) == 2048)
				mCustomName = stream.readUTF();
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
		int size = 6 + mLocation.getSize(true);
		
		int id = mTypeId - (mTypeId & 2048);
		if(id == (EntityType.PLAYER.ordinal() | 1024) || getEntityType().isAlive() && (mTypeId & 2048) == 2048)
			size += Utility.getUTFLength(mCustomName);
		
		return size;
	}
	
	public au.com.mineauz.PlayerSpy.wrappers.minecraft.Entity createEntity()
	{
		au.com.mineauz.PlayerSpy.wrappers.minecraft.Entity ent;
		
		int id = mTypeId - (mTypeId & 2048);
		if(id == (EntityType.PLAYER.ordinal() | 1024))
		{
			ent = new EntityShadowPlayer(CraftWorld.castFrom(mLocation.getLocation().getWorld()).getHandle(), mCustomName);
		}
		else if((id & 1024) == 0)
			ent = EntityTypes.createEntityByID(id, CraftWorld.castFrom(mLocation.getLocation().getWorld()).getHandle());
		else
			return null;
		
		Utility.setEntityPosition(ent, mLocation.getLocation());
		return ent;
	}

	public EntityType getEntityType()
	{
		int id = mTypeId - (mTypeId & 2048);
		if((id & 1024) != 0)
		{
			return EntityType.values()[id & 1023];
		}
		
		return EntityType.fromId(id);
	}
	
	public void setEntityType( EntityType type )
	{
		boolean named = (mTypeId & 2048) == 2048;
		
		mTypeId = type.getTypeId();
		if(mTypeId == -1)
			mTypeId = (short) (1024 | type.ordinal());
		
		if (named)
			mTypeId |= 2048;
	}
	
	public org.bukkit.Location getLocation()
	{
		return mLocation.getLocation();
	}
	public String getCustomName()
	{
		return mCustomName;
	}
	public int getEntityId()
	{
		return mOriginalId;
	}
	
	public String getName()
	{
		if(getEntityType() == EntityType.PLAYER)
			return getCustomName();
		else if(getEntityType().isAlive() && (mTypeId & 2048) == 2048)
		{
			if(mCustomName.isEmpty())
				return getEntityType().getName();
			else
				return mCustomName + " (" + getEntityType().getName() + ")";
		}
		else
			return getEntityType().getName();
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof StoredEntity))
			return false;
		
		StoredEntity entity = (StoredEntity)obj;
		
		if(mTypeId != entity.mTypeId)
			return false;
		
		if(mOriginalId != entity.mOriginalId)
			return false;
		
		if(!mLocation.equals(entity.mLocation))
			return false;
		
		
		if(getEntityType() == EntityType.PLAYER || getEntityType().isAlive())
			return mCustomName.equals(entity.mCustomName);
		return true;
	}
	
	private short mTypeId;
	private StoredLocation mLocation;
	private String mCustomName;
	private int mOriginalId;
}
