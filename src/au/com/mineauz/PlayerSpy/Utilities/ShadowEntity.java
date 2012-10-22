package au.com.mineauz.PlayerSpy.Utilities;

import java.util.Calendar;

import au.com.mineauz.PlayerSpy.StoredEntity;


public class ShadowEntity 
{
	public ShadowEntity(StoredEntity entity, int entityId)
	{
		EntityId = entityId;
		Entity = entity;
		SpawnDate = Calendar.getInstance().getTimeInMillis();
	}
	
	public int EntityId;
	public StoredEntity Entity;
	public long SpawnDate;
}
