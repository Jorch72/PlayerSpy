package au.com.mineauz.PlayerSpy.inspect;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;

import au.com.mineauz.PlayerSpy.SpyPlugin;

public class Inspector implements Listener
{
	public static Inspector instance = new Inspector();
	private HashSet<Player> mInspectors = new HashSet<Player>();
	private HashMap<Player, Location> mSelected = new HashMap<Player, Location>();
	
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
		if(event.getClickedBlock() != null && mInspectors.contains(event.getPlayer()))
		{
			// Right click is the block adjacent to the one we click on
			if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
			{
				// Change which block is selected
				mSelected.put(event.getPlayer(), event.getClickedBlock().getRelative(event.getBlockFace()).getLocation());
				
				inspectBlock(event.getPlayer(), event.getClickedBlock().getRelative(event.getBlockFace()).getLocation());
			}
			// Left click is the block we click on
			else if(event.getAction() == Action.LEFT_CLICK_BLOCK)
			{
				// Change which block is selected
				mSelected.put(event.getPlayer(), event.getClickedBlock().getLocation());
				
				inspectBlock(event.getPlayer(), event.getClickedBlock().getLocation());
			}
			
			event.setCancelled(true);
			event.setUseInteractedBlock(Result.DENY);
		}
	}
}
