package au.com.mineauz.PlayerSpy;

import java.util.Calendar;


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
