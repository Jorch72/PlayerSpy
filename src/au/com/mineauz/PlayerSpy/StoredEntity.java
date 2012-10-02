package au.com.mineauz.PlayerSpy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.server.EntityTypes;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.*;



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
	
	public void read(DataInputStream stream) throws IOException
	{
		// type
		mTypeId = stream.readShort();
		// id
		mOriginalId = stream.readInt();
		// location
		mLocation = StoredLocation.readLocationFull(stream);
		
		// name if player
		if(mTypeId == (EntityType.PLAYER.ordinal() | 1024))
			mPlayerName = stream.readUTF();
	}

	public static StoredEntity readEntity(DataInputStream stream) throws IOException
	{
		StoredEntity ent = new StoredEntity();
		ent.read(stream);
		
		return ent;
	}
	public int getSize()
	{
 		return 6 + mLocation.getSize(true) + (mTypeId == (EntityType.PLAYER.ordinal() | 1024) ? 2 + mPlayerName.length() : 0);
	}
	
	public net.minecraft.server.Entity createEntity()
	{
		net.minecraft.server.Entity ent;
		if(mTypeId == (EntityType.PLAYER.ordinal() | 1024))
		{
			ent = new EntityShadowPlayer(((CraftWorld)mLocation.getLocation().getWorld()).getHandle(), mPlayerName);
		}
		else if((mTypeId & 1024) == 0)
			ent = EntityTypes.a(mTypeId, ((CraftWorld)mLocation.getLocation().getWorld()).getHandle());
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
	
	private short mTypeId;
	private StoredLocation mLocation;
	private String mPlayerName;
	private int mOriginalId;
	
}
