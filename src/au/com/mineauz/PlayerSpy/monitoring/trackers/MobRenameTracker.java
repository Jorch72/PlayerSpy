package au.com.mineauz.PlayerSpy.monitoring.trackers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.NameEntityRecord;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;

public class MobRenameTracker implements Listener, Tracker
{
	public MobRenameTracker()
	{
		Bukkit.getPluginManager().registerEvents(this, SpyPlugin.getInstance());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true)
	public void onEntityClick(PlayerInteractEntityEvent event)
	{
		if(event.getPlayer().getItemInHand() == null || event.getPlayer().getItemInHand().getType() != Material.NAME_TAG || !(event.getRightClicked() instanceof LivingEntity))
			return;
		
		final LivingEntity entity = (LivingEntity)event.getRightClicked();
		
		final Player player = event.getPlayer();
	
		String name = event.getPlayer().getItemInHand().getItemMeta().getDisplayName();
		if(name == null || name.isEmpty())
			return;
		
		final NameEntityRecord record = new NameEntityRecord(entity, name);
		
		final String originalName = entity.getCustomName();
		
		// Log that record if the name actually changed
		Bukkit.getScheduler().runTask(SpyPlugin.getInstance(), new Runnable()
		{
			@Override
			public void run()
			{
				if(entity.isValid())
				{
					if((originalName == null && entity.getCustomName() != null) || (originalName != null && !originalName.equals(entity.getCustomName())))
						GlobalMonitor.instance.logRecord(record, Cause.playerCause(player), null);
				}
			}
		});
	}
}
