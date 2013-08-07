package au.com.mineauz.PlayerSpy.honeypot;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class HoneypotManager implements Listener
{
	public static HoneypotManager instance = new HoneypotManager();
	
	private HashMap<Player, Selection> mSelected = new HashMap<Player, Selection>();
	private HashMap<Player, Long> mLastTime = new HashMap<Player, Long>();

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
				
				if(sel == null || sel.first.getWorld() != event.getPlayer().getWorld())
					event.getPlayer().sendMessage(ChatColor.RED + "Select a block with left click first");
				else
				{
					sel.second = event.getClickedBlock().getLocation().clone();
					int blocks = (Math.abs(sel.first.getBlockX() - sel.second.getBlockX()) + 1) * (Math.abs(sel.first.getBlockY() - sel.second.getBlockY()) + 1) * (Math.abs(sel.first.getBlockZ() - sel.second.getBlockZ()) + 1);
					event.getPlayer().sendMessage("Selected " + ChatColor.GREEN + Utility.locationToStringShorter(sel.first) + ChatColor.WHITE + " to " + ChatColor.GREEN + Utility.locationToStringShorter(sel.second) + ChatColor.DARK_AQUA + " " + blocks + ChatColor.WHITE + " blocks");
				}
			}
			
			mLastTime.put(event.getPlayer(), System.currentTimeMillis());
			
			event.setCancelled(true);
			event.setUseInteractedBlock(Result.DENY);
		}
	}
	
	public void addHoneypot(Honeypot honeypot)
	{
		
	}
	
	public static class Selection
	{
		public Location first;
		public Location second;
		
		public Selection(Location location)
		{
			second = first = location.clone();
		}
		
		public Selection()
		{
		}
		
		public Selection clone()
		{
			Selection sel = new Selection();
			sel.first = first.clone();
			sel.second = second.clone();
			
			return sel;
		}
	}
}
