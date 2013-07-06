package au.com.mineauz.PlayerSpy.monitoring.trackers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.SpawnEgg;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.EntitySpawnRecord;
import au.com.mineauz.PlayerSpy.Records.EntitySpawnRecord.SpawnType;
import au.com.mineauz.PlayerSpy.Utilities.AgeingMap;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;

public class SpawnEggTracker implements Listener, Tracker
{
	private AgeingMap<EntityType, List<Data>> mData;
	
	public SpawnEggTracker()
	{
		mData = new AgeingMap<EntityType, List<Data>>(100);
		Bukkit.getPluginManager().registerEvents(this, SpyPlugin.getInstance());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onEggUse(PlayerInteractEvent event)
	{
		if(!event.hasItem() || event.getItem().getType() != Material.MONSTER_EGG)
			return;

		SpawnEgg egg = (SpawnEgg)event.getItem().getData(); 

		Data data = new Data();
		if(event.hasBlock())
			data.clickedLocation = event.getClickedBlock().getLocation();
		else
			data.clickedLocation = event.getPlayer().getLocation();
		
		data.player = event.getPlayer();
		
		List<Data> dataList = mData.get(egg.getSpawnedType());
		
		if(dataList == null)
		{
			dataList = new ArrayList<Data>();
			mData.put(egg.getSpawnedType(), dataList);
		}
		else
			mData.renew(egg.getSpawnedType());
		
		dataList.add(data);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onSpawn(CreatureSpawnEvent event)
	{
		if(event.getSpawnReason() != SpawnReason.SPAWNER_EGG)
			return;
		
		if(!mData.containsKey(event.getEntityType()))
			return;
		
		Data best = null;
		double bestDist = Double.MAX_VALUE;
		
		List<Data> dataList = mData.get(event.getEntityType());
		for(Data data : dataList)
		{
			if(data.clickedLocation.getWorld().equals(event.getLocation().getWorld()))
			{
				double dist = data.clickedLocation.distanceSquared(event.getLocation());
				if(dist < bestDist)
				{
					best = data;
					bestDist = dist;
				}
				break;
			}
		}
		
		if(best != null)
			GlobalMonitor.instance.logRecord(new EntitySpawnRecord(event.getEntity(), SpawnType.SpawnEgg), Cause.playerCause(best.player), null);
	}
	
	private static class Data
	{
		public Location clickedLocation;
		public Player player;
	}
}
