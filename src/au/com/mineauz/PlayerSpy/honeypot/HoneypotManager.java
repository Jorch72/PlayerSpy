package au.com.mineauz.PlayerSpy.honeypot;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Utilities.BoundingBox;
import au.com.mineauz.PlayerSpy.Utilities.CubicChunk;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class HoneypotManager implements Listener
{
	public static HoneypotManager instance = new HoneypotManager();
	
	private HashMap<Player, Selection> mSelected = new HashMap<Player, Selection>();
	private HashMap<Player, Long> mLastTime = new HashMap<Player, Long>();

	private HashSet<Honeypot> mHoneypots = new HashSet<Honeypot>(); 
	private Multimap<CubicChunk, Honeypot> mChunkLookup = ArrayListMultimap.create();
	
	private Permission mSeePermission;
	
	private HoneypotManager()
	{
		Bukkit.getPluginManager().registerEvents(this, SpyPlugin.getInstance());
		
	}
	
	
	public void toggle(Player player)
	{
		if(mLastTime.containsKey(player))
			disable(player);
		else
			enable(player);
	}
	public void enable(Player player)
	{
		if(!mLastTime.containsKey(player))
		{
			mLastTime.put(player, 0L);
			mSelected.put(player, null);
			player.sendMessage("You are now in honeypot mode");
		}
	}
	
	public void disable(Player player)
	{
		if(mLastTime.remove(player) != null)
		{
			player.sendMessage("You are no longer in honeypot mode");
		}
	}
	
	public Selection getSelection(Player player)
	{
		return mSelected.get(player);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onClick(PlayerInteractEvent event)
	{
		if(event.getClickedBlock() != null && mLastTime.containsKey(event.getPlayer()) && event.getAction() != Action.PHYSICAL)
		{
			long lastTime = mLastTime.get(event.getPlayer());
			
			// Prevent double clicks
			if(System.currentTimeMillis() < lastTime + SpyPlugin.getSettings().inspectTimeout)
			{
				event.setCancelled(true);
				event.setUseInteractedBlock(Result.DENY);
				return;
			}
			
			if(event.getAction() == Action.LEFT_CLICK_BLOCK)
			{
				Selection sel = new Selection(event.getClickedBlock().getLocation());
				
				mSelected.put(event.getPlayer(), sel);
				
				Block block = event.getClickedBlock();
				String name = Utility.formatItemName(new ItemStack(block.getType(), 1, block.getData()));
				event.getPlayer().sendMessage("Selected " + ChatColor.DARK_AQUA + name + ChatColor.WHITE + " at " + ChatColor.GREEN + Utility.locationToStringShorter(block.getLocation()));
			}
			else if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
			{
				Selection sel = mSelected.get(event.getPlayer());
				
				if(sel == null || sel.getWorld() != event.getPlayer().getWorld())
					event.getPlayer().sendMessage(ChatColor.RED + "Select a block with left click first");
				else
				{
					sel.setSecond(event.getClickedBlock().getLocation().clone());
					event.getPlayer().sendMessage("Selected " + ChatColor.GREEN + Utility.locationToStringShorter(sel.first) + ChatColor.WHITE + " to " + ChatColor.GREEN + Utility.locationToStringShorter(sel.second) + ChatColor.DARK_AQUA + " " + sel.getBlockCount() + ChatColor.WHITE + " blocks");
				}
			}
			
			mLastTime.put(event.getPlayer(), System.currentTimeMillis());
			
			event.setCancelled(true);
			event.setUseInteractedBlock(Result.DENY);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onBreakBlock(BlockBreakEvent event)
	{
		CubicChunk chunk = new CubicChunk(event.getBlock().getLocation());
		
		Collection<Honeypot> honeypots = mChunkLookup.get(chunk);
		
		if(honeypots == null)
			return;
		
		for(Honeypot honeypot : honeypots)
		{
			if(!honeypot.region.getWorld().equals(event.getBlock().getWorld()))
				continue;
			
			if(!honeypot.region.getBB().isContained(event.getBlock().getLocation()))
				continue;
			
			Bukkit.broadcastMessage(event.getPlayer().getName() + " broke a honeypot");
			break;
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlaceBlock(BlockPlaceEvent event)
	{
		
	}
	
	public void addHoneypot(Honeypot honeypot)
	{
		mHoneypots.add(honeypot);
		
		for(int x = honeypot.region.getBB().min.getBlockX() >> 4; x <= honeypot.region.getBB().max.getBlockX() >> 4; ++x)
		{
			for(int z = honeypot.region.getBB().min.getBlockZ() >> 4; z <= honeypot.region.getBB().max.getBlockZ() >> 4; ++z)
			{
				for(int y = honeypot.region.getBB().min.getBlockY() >> 4; y <= honeypot.region.getBB().max.getBlockY() >> 4; ++y)
				{
					mChunkLookup.put(new CubicChunk(x,y,z, honeypot.region.first.getWorld()), honeypot);
				}
			}
		}
	}
	
	public void removeHoneypot(Honeypot honeypot)
	{
		throw new RuntimeException("Not implemented");
	}
	
	/**
	 * Used to send particle effects to players that can see honeypots
	 */
	public void onTick()
	{
		// TODO: This permissions is not working for some reason
		if(mSeePermission == null)
			mSeePermission = Bukkit.getPluginManager().getPermission("playerspy.honeypot.immune");
		
		// TODO: This needs to be more optimized
		for(Player player : Bukkit.getOnlinePlayers())
		{
			if(true) // This should check the permission
			{
				// Get the honeypots near them
				HashSet<CubicChunk> chunks = new HashSet<CubicChunk>();
				
				for(int x = (player.getLocation().getBlockX() >> 4) - 2; x <= (player.getLocation().getBlockX() >> 4) + 2; ++x)
				{
					for(int z = (player.getLocation().getBlockZ() >> 4) - 2; z <= (player.getLocation().getBlockZ() >> 4) + 2; ++z)
					{
						for(int y = (player.getLocation().getBlockY() >> 4) - 2; y <= (player.getLocation().getBlockY() >> 4) + 2; ++y)
						{
							chunks.add(new CubicChunk(x,y,z, player.getWorld()));
						}
					}
				}
				
				HashSet<Honeypot> honeypots = new HashSet<Honeypot>();
				for(CubicChunk chunk : chunks)
				{
					Collection<Honeypot> inChunk = mChunkLookup.get(chunk);
					if(inChunk != null)
						honeypots.addAll(inChunk);
				}
				
				Random r = new Random();
				// Display particles on them
				for(Honeypot honeypot : honeypots)
				{
					for(int x = honeypot.region.getBB().min.getBlockX(); x <= honeypot.region.getBB().max.getBlockX(); ++x)
					{
						for(int z = honeypot.region.getBB().min.getBlockZ(); z <= honeypot.region.getBB().max.getBlockZ(); ++z)
						{
							for(int y = honeypot.region.getBB().min.getBlockY(); y <= honeypot.region.getBB().max.getBlockY(); ++y)
							{
								Location loc = new Location(player.getWorld(), x, y, z);
								
								if(player.getLocation().distanceSquared(loc) < 100 && r.nextInt(100) == 0)
								{
									player.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
								}
							}
						}
					}
				}
				
				
			}
		}
	}
	
	public static class Selection
	{
		private Location first;
		private Location second;
		
		private BoundingBox boundingBox;
		
		public Selection(Location location)
		{
			second = first = location.clone();
			boundingBox = new BoundingBox();
			boundingBox.addPoint(location);
		}
		
		public Selection()
		{
		}
		
		public Location getFirst()
		{
			return first;
		}
		
		public Location getSecond()
		{
			return second;
		}
		
		public BoundingBox getBB()
		{
			return boundingBox;
		}
		
		public World getWorld()
		{
			return first.getWorld();
		}
		
		public void setFirst(Location location)
		{
			first = location.clone();
			boundingBox = new BoundingBox();
			boundingBox.addPoint(location);
			if(second != null)
				boundingBox.addPoint(second);
		}
		
		public void setSecond(Location location)
		{
			second = location.clone();
			boundingBox = new BoundingBox();
			boundingBox.addPoint(location);
			if(first != null)
				boundingBox.addPoint(first);
		}
		
		public int getBlockCount()
		{
			return (boundingBox.max.getBlockX() - boundingBox.min.getBlockX() + 1) *
					(boundingBox.max.getBlockY() - boundingBox.min.getBlockY() + 1) * 
					(boundingBox.max.getBlockZ() - boundingBox.min.getBlockZ() + 1);
		}
		
		public Selection clone()
		{
			Selection sel = new Selection();
			sel.first = first.clone();
			sel.setSecond(second);
			
			return sel;
		}
	}
}
