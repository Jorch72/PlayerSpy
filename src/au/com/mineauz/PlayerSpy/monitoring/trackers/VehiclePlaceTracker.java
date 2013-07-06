package au.com.mineauz.PlayerSpy.monitoring.trackers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.EntitySpawnRecord;
import au.com.mineauz.PlayerSpy.Records.EntitySpawnRecord.SpawnType;
import au.com.mineauz.PlayerSpy.Utilities.AgeingMap;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;

public class VehiclePlaceTracker implements Listener, Tracker
{
	private AgeingMap<EntityType, List<Data>> mData;
	
	public VehiclePlaceTracker()
	{
		mData = new AgeingMap<EntityType, List<Data>>(100);
		Bukkit.getPluginManager().registerEvents(this, SpyPlugin.getInstance());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onVehiclePlace(PlayerInteractEvent event)
	{
		if(!event.hasItem())
			return;

		
		EntityType expected = null;
		
		switch(event.getItem().getType())
		{
		case BOAT:
			expected = EntityType.BOAT;
			break;
		case MINECART:
			expected = EntityType.MINECART;
			break;
		case EXPLOSIVE_MINECART:
			expected = EntityType.MINECART_TNT;
			break;
		case STORAGE_MINECART:
			expected = EntityType.MINECART_CHEST;
			break;
		case POWERED_MINECART:
			expected = EntityType.MINECART_FURNACE;
			break;
		case HOPPER_MINECART:
			expected = EntityType.MINECART_HOPPER;
			break;
		default:
			return;
		}
		
		Data data = new Data();
		if(event.hasBlock())
			data.clickedLocation = event.getClickedBlock().getLocation();
		else
			data.clickedLocation = event.getPlayer().getLocation();
		
		data.player = event.getPlayer();
		
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
	public void onSpawn(VehicleCreateEvent event)
	{
		if(!mData.containsKey(event.getVehicle().getType()))
			return;
		
		Data best = null;
		double bestDist = Double.MAX_VALUE;
		
		List<Data> dataList = mData.get(event.getVehicle().getType());
		for(Data data : dataList)
		{
			if(data.clickedLocation.getWorld().equals(event.getVehicle().getLocation().getWorld()))
			{
				double dist = data.clickedLocation.distanceSquared(event.getVehicle().getLocation());
				if(dist < bestDist)
				{
					best = data;
					bestDist = dist;
				}
				break;
			}
		}
		
		if(best != null)
			GlobalMonitor.instance.logRecord(new EntitySpawnRecord(event.getVehicle(), SpawnType.Place), Cause.playerCause(best.player), null);
	}
	
	private static class Data
	{
		public Location clickedLocation;
		public Player player;
	}
}
