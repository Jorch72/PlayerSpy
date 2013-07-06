package au.com.mineauz.PlayerSpy.monitoring.trackers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerEggThrowEvent;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.EntitySpawnRecord;
import au.com.mineauz.PlayerSpy.Records.EntitySpawnRecord.SpawnType;
import au.com.mineauz.PlayerSpy.Utilities.AgeingMap;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;

public class EggTracker implements Listener, Tracker
{
	private AgeingMap<EntityType, List<Data>> mData;
	
	public EggTracker()
	{
		mData = new AgeingMap<EntityType, List<Data>>(10000);
		Bukkit.getPluginManager().registerEvents(this, SpyPlugin.getInstance());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onEggThrow(PlayerEggThrowEvent event)
	{
		if(!event.isHatching() || event.getNumHatches() == 0)
			return;
		
		Data data = new Data();
		data.entityId = event.getEgg().getEntityId();
		data.player = event.getPlayer();
		
		List<Data> dataList = mData.get(event.getHatchingType());
		
		if(dataList == null)
		{
			dataList = new ArrayList<Data>();
			mData.put(event.getHatchingType(), dataList);
		}
		else
			mData.renew(event.getHatchingType());
		
		dataList.add(data);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onEggHatch(CreatureSpawnEvent event)
	{
		if(event.getSpawnReason() != SpawnReason.EGG)
			return;
		
		if(!mData.containsKey(event.getEntityType()))
			return;
		
		// Find the egg
		List<Entity> entities = event.getEntity().getNearbyEntities(8, 4, 8);
		Egg nearest = null;
		double dist = Double.MAX_VALUE;
		
		for(Entity ent : entities)
		{
			if(ent instanceof Egg)
			{
				double d = ent.getLocation().distanceSquared(event.getLocation());
				if(d < dist)
				{
					dist = d;
					nearest = (Egg)ent;
				}
			}
		}
		
		if(nearest == null)
			return;
		
		// See if we have a record for this egg
		List<Data> dataList = mData.get(event.getEntityType());
		for(Data data : dataList)
		{
			if(data.entityId == nearest.getEntityId())
			{
				GlobalMonitor.instance.logRecord(new EntitySpawnRecord(event.getEntity(), SpawnType.Egg), Cause.playerCause(data.player), null);
				break;
			}
		}
	}
	
	private static class Data
	{
		public int entityId;
		public Player player;
	}
}
