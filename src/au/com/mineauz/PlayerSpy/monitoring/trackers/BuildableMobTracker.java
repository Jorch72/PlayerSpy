package au.com.mineauz.PlayerSpy.monitoring.trackers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.EntitySpawnRecord;
import au.com.mineauz.PlayerSpy.Records.EntitySpawnRecord.SpawnType;
import au.com.mineauz.PlayerSpy.Utilities.AgeingMap;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;

public class BuildableMobTracker implements Listener, Tracker
{
	private AgeingMap<EntityType, List<Data>> mData;
	
	public BuildableMobTracker()
	{
		mData = new AgeingMap<EntityType, List<Data>>(100);
		Bukkit.getPluginManager().registerEvents(this, SpyPlugin.getInstance());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onBlockPlaced(BlockPlaceEvent event)
	{
		if(event.getBlockPlaced().getType() != Material.PUMPKIN && event.getBlockPlaced().getType() != Material.SKULL)
			return;
		
		Data data = new Data();
		data.player = event.getPlayer();
		data.clickedLocation = event.getBlock().getLocation();
		
		EntityType expected = null;
		
		if(event.getBlockPlaced().getType() == Material.PUMPKIN)
		{
			switch(event.getBlockPlaced().getRelative(BlockFace.DOWN).getType())
			{
			case SNOW_BLOCK:
				expected = EntityType.SNOWMAN;
				break;
			case IRON_BLOCK:
				expected = EntityType.IRON_GOLEM;
				break;
			default:
				break;
			}
		}
		else if(event.getBlockPlaced().getType() == Material.SKULL)
		{
			if(event.getBlockPlaced().getRelative(BlockFace.DOWN).getType() == Material.SOUL_SAND)
				expected = EntityType.WITHER;
		}
		
		if(expected == null)
			return;
		
		List<Data> dataList = mData.get(expected);
		
		if(dataList == null)
		{
			dataList = new ArrayList<Data>();
			mData.put(expected, dataList);
		}
		else
			mData.renew(expected);
		
		dataList.add(data);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onSpawn(CreatureSpawnEvent event)
	{
		if(event.getSpawnReason() != SpawnReason.BUILD_IRONGOLEM && event.getSpawnReason() != SpawnReason.BUILD_SNOWMAN && event.getSpawnReason() != SpawnReason.BUILD_WITHER)
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
			GlobalMonitor.instance.logRecord(new EntitySpawnRecord(event.getEntity(), SpawnType.Place), Cause.playerCause(best.player), null);
	}
	
	private static class Data
	{
		public Location clickedLocation;
		public Player player;
	}
}
