package au.com.mineauz.PlayerSpy.inspect;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
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
	private HashMap<Player, InspectInfo> mInspectors = new HashMap<Player, InspectInfo>();
	private HashMap<Player, Location> mSelected = new HashMap<Player, Location>();
	
	private Inspector()
	{
		Bukkit.getPluginManager().registerEvents(this, SpyPlugin.getInstance());
	}
	public void toggleInspect(Player player)
	{
		if(mInspectors.containsKey(player))
			disableInspect(player);
		else
			enableInspect(player);
	}
	public void enableInspect(Player player)
	{
		if(!mInspectors.containsKey(player))
		{
			InspectInfo info = new InspectInfo();
			info.loadDefaults();
			info.lastInspectTime = 0L;
			
			mInspectors.put(player, info);
			mSelected.put(player, null);
			player.sendMessage("You are now in inspect mode");
		}
	}
	public void enableInspect(Player player, InspectInfo settings)
	{
		if(!mInspectors.containsKey(player))
		{
			if(settings.itemCount <= 0)
				settings.itemCount = SpyPlugin.getSettings().inspectCount;
			
			settings.lastInspectTime = 0L;
			
			mInspectors.put(player, settings);
			mSelected.put(player, null);
			player.sendMessage("You are now in inspect mode");
		}
	}
	public void updateInspect(Player player, InspectInfo settings)
	{
		if(!mInspectors.containsKey(player))
			return;
		
		InspectInfo existing = mInspectors.get(player);
		settings.lastInspectTime = existing.lastInspectTime;
		mInspectors.put(player, settings);
		
		player.sendMessage("Updated inspect settings");
	}
	
	public void disableInspect(Player player)
	{
		if(mInspectors.remove(player) != null)
		{
			player.sendMessage("You are no longer in inspect mode");
		}
	}
	
	public boolean isInspecting(Player player)
	{
		return mInspectors.containsKey(player);
	}
	public Location getSelectedBlock(Player player)
	{
		return mSelected.get(player);
	}
	
	private void inspectBlock(Player player, Location loc, InspectInfo settings)
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
		SpyPlugin.getExecutor().submit(new InspectBlockTask(player, loc, altLocation, altType, settings));
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onClick(PlayerInteractEvent event)
	{
		if(event.getClickedBlock() != null && mInspectors.containsKey(event.getPlayer()) && event.getAction() != Action.PHYSICAL)
		{
			InspectInfo inspectInfo = mInspectors.get(event.getPlayer());
			
			// Prevent double clicks
			if(System.currentTimeMillis() < inspectInfo.lastInspectTime + SpyPlugin.getSettings().inspectTimeout)
			{
				event.setCancelled(true);
				event.setUseInteractedBlock(Result.DENY);
				return;
			}
				
			// Right click is the block adjacent to the one we click on
			if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
				inspectBlock(event.getPlayer(), event.getClickedBlock().getRelative(event.getBlockFace()).getLocation(), inspectInfo);
			
			// Left click is the block we click on
			else if(event.getAction() == Action.LEFT_CLICK_BLOCK)
				inspectBlock(event.getPlayer(), event.getClickedBlock().getLocation(), inspectInfo);
			
			inspectInfo.lastInspectTime = System.currentTimeMillis();
			
			event.setCancelled(true);
			event.setUseInteractedBlock(Result.DENY);
		}
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onClickEntity(PlayerInteractEntityEvent event)
	{
		if(mInspectors.containsKey(event.getPlayer()))
		{
			// Prevent double clicks
			if(System.currentTimeMillis() < mInspectors.get(event.getPlayer()).lastInspectTime + SpyPlugin.getSettings().inspectTimeout)
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
		if(mInspectors.containsKey(player))
		{
			// Prevent double clicks
			if(System.currentTimeMillis() < mInspectors.get(player).lastInspectTime + SpyPlugin.getSettings().inspectTimeout)
			{
				event.setCancelled(true);
				return;
			}
			// Check the information about this entity
			ArrayList<String> output = new ArrayList<String>();
			output.add(ChatColor.GOLD + "[PlayerSpy] " + ChatColor.WHITE + "Entity information " + event.getEntity().getType().getName() + " (" + event.getEntity().getEntityId() + ") WIP");
			if(event.getEntity() instanceof Tameable)
			{
				output.add(ChatColor.DARK_AQUA + "Is Tamed: " + ChatColor.RED + ((Tameable)event.getEntity()).isTamed());
				if(((Tameable)event.getEntity()).isTamed())
					output.add(ChatColor.DARK_AQUA + "Owner: " + ChatColor.RED + ((Tameable)event.getEntity()).getOwner().getName());
			}
			
			if(event.getEntity() instanceof LivingEntity)
			{
				LivingEntity ent = (LivingEntity)event.getEntity();
				
				if(ent.getLastDamageCause() instanceof EntityDamageByEntityEvent)
				{
					EntityDamageByEntityEvent damage = (EntityDamageByEntityEvent)ent.getLastDamageCause();
					
					if(damage.getDamager() instanceof Player)
						output.add(ChatColor.DARK_AQUA + "Last Damager: " + ChatColor.RED + ((Player)damage.getDamager()).getName());
					else if(damage.getDamager() instanceof Projectile && ((Projectile)damage.getDamager()).getShooter() instanceof Player)
						output.add(ChatColor.DARK_AQUA + "Last Damager: " + ChatColor.RED + ((Player)((Projectile)damage.getDamager()).getShooter()).getName());
				}
			}
			
			for(String line : output)
				player.sendMessage(line);
			
			event.setCancelled(true);
		}
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onClickPainting(HangingBreakByEntityEvent event)
	{
		if(!(event.getRemover() instanceof Player))
			return;
		
		Player player = (Player)event.getRemover();
		
		if(mInspectors.containsKey(player))
		{
			// Prevent double clicks
			if(System.currentTimeMillis() < mInspectors.get(player).lastInspectTime + SpyPlugin.getSettings().inspectTimeout)
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
		
		if(mInspectors.containsKey(player))
		{
			// Prevent double clicks
			if(System.currentTimeMillis() <mInspectors.get(player).lastInspectTime + SpyPlugin.getSettings().inspectTimeout)
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
		inspectBlock(player, loc, mInspectors.get(player));
		
		mInspectors.get(player).lastInspectTime = System.currentTimeMillis();
	}
}
