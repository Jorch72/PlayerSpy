package au.com.mineauz.PlayerSpy.inspect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import au.com.mineauz.PlayerSpy.SpyPlugin;

public class Inspector implements Listener
{
	public static final ExecutorService AsyncService = Executors.newSingleThreadExecutor();
	
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
		AsyncService.submit(new InspectBlockTask(player, loc));
	}
	@EventHandler(priority = EventPriority.MONITOR)
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
		}
	}
}
