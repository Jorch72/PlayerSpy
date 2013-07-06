package au.com.mineauz.PlayerSpy.monitoring.trackers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.EntitySpawnRecord;
import au.com.mineauz.PlayerSpy.Records.EntitySpawnRecord.SpawnType;
import au.com.mineauz.PlayerSpy.Utilities.AgeingMap;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;

public class BreedingTracker implements Listener, Tracker
{
	private AgeingMap<EntityType, List<Data>> mData;
	
	public BreedingTracker()
	{
		mData = new AgeingMap<EntityType, List<Data>>(10000);
		Bukkit.getPluginManager().registerEvents(this, SpyPlugin.getInstance());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true)
	public void onAnimalClick(PlayerInteractEntityEvent event)
	{
		Data data = new Data();
		data.player = event.getPlayer();
		data.entityId = -1;
		
		if(event.getRightClicked() instanceof Cow && (event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getType() == Material.WHEAT))
			data.entityId = event.getRightClicked().getEntityId();
		else if(event.getRightClicked() instanceof Pig && (event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getType() == Material.CARROT_ITEM))
			data.entityId = event.getRightClicked().getEntityId();
		// TODO: Check that this should actually be seeds
		else if(event.getRightClicked() instanceof Chicken && (event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getType() == Material.SEEDS))
			data.entityId = event.getRightClicked().getEntityId();
		
		if(data.entityId == -1)
			return;
		
		List<Data> dataList = mData.get(event.getRightClicked().getType());
		
		if(dataList == null)
		{
			dataList = new ArrayList<Data>();
			mData.put(event.getRightClicked().getType(), dataList);
		}
		else
			mData.renew(event.getRightClicked().getType());
		
		dataList.add(data);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true)
	public void onAnimalBred(CreatureSpawnEvent event)
	{
		if(event.getSpawnReason() != SpawnReason.BREEDING)
			return;
		
		if(!mData.containsKey(event.getEntityType()))
			return;
		
		// Guess which animal is the parent
		List<Entity> entities = event.getEntity().getNearbyEntities(8, 4, 8);
		Animals nearest = null;
		double dist = Double.MAX_VALUE;
		
		for(Entity ent : entities)
		{
			if(ent instanceof Animals && ((Animals)ent).isAdult() && ent.getType() == event.getEntityType())
			{
				double d = ent.getLocation().distanceSquared(event.getLocation());
				if(d < dist)
				{
					dist = d;
					nearest = (Animals)ent;
				}
			}
		}
		
		if(nearest == null)
			return;
		
		// See if we have a record for this animal
		List<Data> dataList = mData.get(event.getEntityType());
		for(Data data : dataList)
		{
			if(data.entityId == nearest.getEntityId())
			{
				GlobalMonitor.instance.logRecord(new EntitySpawnRecord(event.getEntity(), SpawnType.Breeding), Cause.playerCause(data.player), null);
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
