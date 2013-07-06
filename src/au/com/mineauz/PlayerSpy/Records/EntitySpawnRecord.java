package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import au.com.mineauz.PlayerSpy.storage.StoredEntity;

public class EntitySpawnRecord extends Record implements ILocationAware 
{
	public enum SpawnType
	{
		Place, 
		SpawnEgg,
		Egg,
		Breeding
	}
	
	private StoredEntity mSpawned;
	private SpawnType mType;
	
	
	public EntitySpawnRecord(Entity spawned, SpawnType type) 
	{
		super(RecordType.EntitySpawn);
		
		mSpawned = new StoredEntity(spawned);
		mType = type;
	}
	public EntitySpawnRecord()
	{
		super(RecordType.EntitySpawn);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeByte((byte)mType.ordinal());
		mSpawned.write(stream);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException 
	{
		mType = SpawnType.values()[stream.readByte()];
		mSpawned = StoredEntity.readEntity(stream);
	}

	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 1 + mSpawned.getSize();
	}

	public StoredEntity getEntity()
	{
		return mSpawned;
	}
	
	public SpawnType getSpawnType()
	{
		return mType;
	}
	
	@Override
	public String getDescription()
	{
		switch(mType)
		{
		case Egg:
			return "%s created a " + ChatColor.DARK_AQUA + mSpawned.getName() + ChatColor.RESET + " from an egg";
		case Place:
			return "%s placed a " + ChatColor.DARK_AQUA + mSpawned.getName() + ChatColor.RESET;
		case Breeding:
			return "%s bred a " + ChatColor.DARK_AQUA + mSpawned.getName() + ChatColor.RESET;
		case SpawnEgg:
			return "%s spawned a " + ChatColor.DARK_AQUA + mSpawned.getName() + ChatColor.RESET;
		}
		return null;
	}
	@Override
	public Location getLocation()
	{
		return mSpawned.getLocation();
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof EntitySpawnRecord))
			return false;
		
		EntitySpawnRecord record = (EntitySpawnRecord)obj;
		
		return mSpawned.equals(record.mSpawned) && mType == record.mType;
	}
	
	@Override
	public String toString()
	{
		return "EntitySpawnRecord { type: " + mType + " entity: " + mSpawned.toString() + " }";
	}
	
}
