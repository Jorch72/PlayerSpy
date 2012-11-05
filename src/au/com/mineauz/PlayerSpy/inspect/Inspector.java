package au.com.mineauz.PlayerSpy.inspect;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.DoubleChestInventory;

import au.com.mineauz.PlayerSpy.SpyPlugin;

public class Inspector implements Listener
{
	public static Inspector instance = new Inspector();
	private HashSet<Player> mInspectors = new HashSet<Player>();
	private HashMap<Player, Location> mSelected = new HashMap<Player, Location>();
	private HashMap<Player, Long> mLastInspectTime = new HashMap<Player, Long>();
	
	private Inspector()
	{
		Bukkit.getPluginManager().registerEvents(this, SpyPlugin.getInstance());
	}
	public void toggleInspect(Player player)
	{
		if(mInspectors.contains(player))
			disableInspect(player);
		else
			enableInspect(player);
	}
	public void enableInspect(Player player)
	{
		if(!mInspectors.contains(player))
		{
			mInspectors.add(player);
			mSelected.put(player, null);
			mLastInspectTime.put(player, 0L);
			player.sendMessage("You are now in inspect mode");
		}
	}
	
	public void disableInspect(Player player)
	{
		if(mInspectors.remove(player))
		{
			player.sendMessage("You are no longer in inspect mode");
		}
	}
	public Location getSelectedBlock(Player player)
	{
		return mSelected.get(player);
	}
	
	private void inspectBlock(Player player, Location loc)
	{
		// Change which block is selected
		mSelected.put(player, loc);
		
		Location altLocation = null;
		Material altType = null;
		// Do alternate inspect locations for multi block items
		if(loc.getBlock().getType() == Material.WOODEN_DOOR || loc.getBlock().getType() == Material.IRON_DOOR_BLOCK)
		{
			if((loc.getBlock().getData() & 8) != 0) // Top half of door
				altLocation = loc.getBlock().getRelative(BlockFace.DOWN).getLocation().clone();
			else // Bottom half of door
				altLocation = loc.getBlock().getRelative(BlockFace.UP).getLocation().clone();
			
			altType = loc.getBlock().getType();
		}
		else if(loc.getBlock().getType() == Material.CHEST)
		{
			Chest c = (Chest)loc.getBlock().getState();
			if(c.getInventory() instanceof DoubleChestInventory)
			{
				Chest left = (Chest)((DoubleChestInventory)c.getInventory()).getLeftSide().getHolder();
				Chest right = (Chest)((DoubleChestInventory)c.getInventory()).getRightSide().getHolder();
				
				if(loc.equals(left.getLocation()))
					altLocation = right.getLocation().clone();
				else
					altLocation = left.getLocation().clone();
			}
			
			altType = loc.getBlock().getType();
		}
		SpyPlugin.getExecutor().submit(new InspectBlockTask(player, loc, altLocation, altType));
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onClick(PlayerInteractEvent event)
	{
		if(event.getClickedBlock() != null && mInspectors.contains(event.getPlayer()) && event.getAction() != Action.PHYSICAL)
		{
			// Prevent double clicks
			if(System.currentTimeMillis() < mLastInspectTime.get(event.getPlayer()) + SpyPlugin.getSettings().inspectTimeout)
			{
				event.setCancelled(true);
				event.setUseInteractedBlock(Result.DENY);
				return;
			}
				
			// Right click is the block adjacent to the one we click on
			if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
				inspectBlock(event.getPlayer(), event.getClickedBlock().getRelative(event.getBlockFace()).getLocation());
			
			// Left click is the block we click on
			else if(event.getAction() == Action.LEFT_CLICK_BLOCK)
				inspectBlock(event.getPlayer(), event.getClickedBlock().getLocation());
			
			mLastInspectTime.put(event.getPlayer(), System.currentTimeMillis());
			
			event.setCancelled(true);
			event.setUseInteractedBlock(Result.DENY);
		}
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onClickEntity(PlayerInteractEntityEvent event)
	{
		if(mInspectors.contains(event.getPlayer()))
		{
			// Prevent double clicks
			if(System.currentTimeMillis() < mLastInspectTime.get(event.getPlayer()) + SpyPlugin.getSettings().inspectTimeout)
			{
				event.setCancelled(true);
				return;
			}
						
			doInspectAtEntity(event.getRightClicked(), event.getPlayer());
			
			event.setCancelled(true);
		}
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onHitEntity(EntityDamageByEntityEvent event)
	{
		if(!(event.getDamager() instanceof Player))
			return;
		
		Player player = (Player)event.getDamager();
		if(mInspectors.contains(player))
		{
			// Prevent double clicks
			if(System.currentTimeMillis() < mLastInspectTime.get(player) + SpyPlugin.getSettings().inspectTimeout)
			{
				event.setCancelled(true);
				return;
			}
						
			doInspectAtEntity(event.getEntity(), player);
			
			event.setCancelled(true);
		}
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onClickPainting(HangingBreakByEntityEvent event)
	{
		if(!(event.getRemover() instanceof Player))
			return;
		
		Player player = (Player)event.getRemover();
		
		if(mInspectors.contains(player))
		{
			// Prevent double clicks
			if(System.currentTimeMillis() < mLastInspectTime.get(player) + SpyPlugin.getSettings().inspectTimeout)
			{
				event.setCancelled(true);
				return;
			}
						
			doInspectAtEntity(event.getEntity(),player);
			
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onClickVehicle(VehicleDamageEvent event)
	{
		if(!(event.getAttacker() instanceof Player))
			return;
		
		Player player = (Player)event.getAttacker();
		
		if(mInspectors.contains(player))
		{
			// Prevent double clicks
			if(System.currentTimeMillis() < mLastInspectTime.get(player) + SpyPlugin.getSettings().inspectTimeout)
			{
				event.setCancelled(true);
				return;
			}
						
			doInspectAtEntity(event.getVehicle(),player);
			
			event.setCancelled(true);
		}
	}
	
	private void doInspectAtEntity(Entity entity,Player player)
	{
		Location loc = entity.getLocation().getBlock().getLocation();
		inspectBlock(player, loc);
		
		mLastInspectTime.put(player, System.currentTimeMillis());
	}
}
