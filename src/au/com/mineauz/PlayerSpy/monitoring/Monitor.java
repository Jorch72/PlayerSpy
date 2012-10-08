package au.com.mineauz.PlayerSpy.monitoring;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.zip.GZIPOutputStream;

import net.minecraft.server.EntityThrownExpBottle;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.painting.PaintingBreakByEntityEvent;
import org.bukkit.event.painting.PaintingBreakEvent;
import org.bukkit.event.painting.PaintingBreakEvent.RemoveCause;
import org.bukkit.event.painting.PaintingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Utility;
import au.com.mineauz.PlayerSpy.Records.*;
import au.com.mineauz.PlayerSpy.legacy.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.legacy.InteractRecord;
import au.com.mineauz.PlayerSpy.legacy.StoredBlock;
import au.com.mineauz.PlayerSpy.legacy.UpdateInventoryRecord;

@SuppressWarnings({"unused", "deprecation"})
public class Monitor implements Listener
{
	public Monitor(SpyPlugin plugin, String player, LogFile logfile)
	{
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		mPlayerName = player;
		mPlugin = plugin;
		
		// Attempt to get the player
		mPlayerInstance = plugin.getServer().getPlayerExact(player);
		
		if(mPlayerInstance != null)
		{
			mLastPlayerLocation = mPlayerInstance.getLocation().clone();
			mLastPlayerHeadLocation = mPlayerInstance.getEyeLocation().clone();
			startMonitoring();
			
			// do a world change so we know what world they are in
			mRecords.add(new WorldChangeRecord(mPlayerInstance.getWorld()));
			recordInventoryChange(new InventoryRecord(mPlayerInstance.getInventory()));
		}
		
		mLogFile = logfile;
		mLogFile.addReference();
	}
	
	public void stop()
	{
		if(mRecording)
		{
			if(mMonitorId != -1)
				mPlugin.getServer().getScheduler().cancelTask(mMonitorId);
			
			mRecording = false;
			flushData();
			mLogFile.close(true);
			mLogFile = null;
		}
	}
	public boolean isRecording() { return mRecording; }
	public String getPlayer() { return mPlayerName; }
	
	@EventHandler(priority = EventPriority.LOWEST,ignoreCancelled = true)
	private void onPlayerJoin(PlayerLoginEvent event)
	{
		if(event.getResult() != Result.ALLOWED)
			return;
		
		// See if they logged in
		if(event.getPlayer().getName().compareToIgnoreCase(mPlayerName) == 0)
		{
			mPlayerInstance = event.getPlayer();
			mLastPlayerLocation = mPlayerInstance.getLocation().clone();
			mLastPlayerHeadLocation = mPlayerInstance.getEyeLocation().clone();
			
			mRecords.add(new LoginRecord(mPlayerInstance.getLocation()));
			mPlugin.getServer().getScheduler().cancelTask(mKillTaskId);
			mRecords.add(new InventoryRecord(mPlayerInstance.getInventory()));
			startMonitoring();
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST,ignoreCancelled = true)
	private void onPlayerLeave(PlayerQuitEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mPlayerInstance = null;
			mRecords.add(new LogoffRecord(LogoffRecord.LogoffType.Quit,""));
			mPlugin.getServer().getScheduler().cancelTask(mMonitorId);
			mKillTaskId = mPlugin.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
				
				@Override
				public void run() {
					mPlugin.removeMonitor(mPlayerName);
				}
			}, 2400L);
			mMonitorId = -1;
			tryFlush();
			
		}
	}
	@EventHandler(priority = EventPriority.LOWEST,ignoreCancelled = true)
	private void onPlayerKicked(PlayerKickEvent event)
	{
		if(!mRecording)
			return;
		if(event.isCancelled())
			return;
		
		if(event.getPlayer() == mPlayerInstance)
		{
			mPlayerInstance = null;
			if(event.getPlayer().isBanned())
				mRecords.add(new LogoffRecord(LogoffRecord.LogoffType.Ban,event.getReason()));
			else
				mRecords.add(new LogoffRecord(LogoffRecord.LogoffType.Kick,event.getReason()));
			mPlugin.getServer().getScheduler().cancelTask(mMonitorId);
			mKillTaskId = mPlugin.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
				
				@Override
				public void run() {
					mPlugin.removeMonitor(mPlayerName);
				}
			}, 2400L);
			mMonitorId = -1;
			tryFlush();
		}
	}
	
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onEntityDamage(EntityDamageByEntityEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getEntity() == mPlayerInstance)
		{
			if(event.getDamager() instanceof Projectile)
			{
				// We are being damaged
				LogUtil.finest("Target was shot by " + ((Projectile)event.getDamager()).getShooter().getType().toString());
				mRecords.add(new DamageRecord(((Projectile)event.getDamager()).getShooter(),event.getDamage()));
			}
			else
			{
				// We are being damaged
				LogUtil.finest("Target was damaged by entity");
				mRecords.add(new DamageRecord(event.getDamager(),event.getDamage()));
			}
		}
		else if(event.getDamager() == mPlayerInstance)
		{
			// We are damaging another entity
			LogUtil.finest("Target attacked " + (event.getEntity() instanceof Player ? ((Player)event.getEntity()).getName() : event.getEntityType().getName()));
			mRecords.add(new AttackRecord(event.getEntity(),event.getDamage()));
		}
		else if(event.getDamager() instanceof Projectile && ((Projectile)event.getDamager()).getShooter() == mPlayerInstance)
		{
			// We are damaging another entity
			LogUtil.finest("Target shot " + (event.getEntity() instanceof Player ? ((Player)event.getEntity()).getName() : event.getEntityType().getName()));
			mRecords.add(new AttackRecord(event.getEntity(),event.getDamage()));
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onEntityDamage(EntityDamageByBlockEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getEntity() == mPlayerInstance)
		{
			mRecords.add(new DamageRecord(null,event.getDamage()));
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onEntityDamage(EntityDamageEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getEntity() == mPlayerInstance)
		{
			mRecords.add(new DamageRecord(null,event.getDamage()));
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerInteractEntity(PlayerInteractEntityEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new InteractRecord(Action.RIGHT_CLICK_AIR,null, null, event.getRightClicked()));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerInteract(PlayerInteractEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new InteractRecord(event.getAction(), event.getClickedBlock(), event.getItem(), null));
			scheduleInvUpdate();
			tryFlush();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerPickupItem(PlayerPickupItemEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new ItemPickupRecord(event.getItem()));
			scheduleInvUpdate();
			tryFlush();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerChangeWorld(PlayerChangedWorldEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new WorldChangeRecord(event.getPlayer().getWorld()));
			scheduleInvUpdate();
			mLastPlayerLocation = mPlayerInstance.getLocation().clone();
			mLastPlayerHeadLocation = mPlayerInstance.getEyeLocation().clone();
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerChat(AsyncPlayerChatEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new ChatCommandRecord(event.getMessage(), event.isCancelled()));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerCommand(PlayerCommandPreprocessEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new ChatCommandRecord(event.getMessage(), event.isCancelled()));
			tryFlush();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new GameModeRecord(event.getNewGameMode().getValue()));
			tryFlush();
		}
	}
	
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerTeleport(PlayerTeleportEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new TeleportRecord(event.getTo(), event.getCause()));
			tryFlush();
		}
	}
	
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerDeath(PlayerDeathEvent event)
	{
		if(!mRecording)
			return;
		if(event.getEntity() == mPlayerInstance)
		{
			mRecords.add(new DeathRecord(event.getEntity().getLocation(),event.getDeathMessage()));
			recordInventoryChange(new InventoryRecord(mPlayerInstance.getInventory()));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST)
	private void onEntityDeath(EntityDeathEvent event)
	{
		if(!mRecording)
			return;

		if(event.getEntity().getKiller() == mPlayerInstance)
		{
			// We killed them
			LogUtil.finest("Target killed " + event.getEntity().getType().getName());
			mRecords.add(new AttackRecord(event.getEntity(), -1));
			tryFlush();
		}

	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerRespawn(PlayerRespawnEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new RespawnRecord(event.getRespawnLocation()));
			recordInventoryChange(new InventoryRecord(mPlayerInstance.getInventory()));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerAnimationEvent(PlayerAnimationEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			if(event.getAnimationType() == PlayerAnimationType.ARM_SWING)
			{
				mRecords.add(new ArmSwingRecord());
				tryFlush();
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerDropItemEvent(PlayerDropItemEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new DropItemRecord(event.getItemDrop().getItemStack()));
			recordInventoryChange(new InventoryRecord(mPlayerInstance.getInventory()));
			//scheduleInvUpdate();
			
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerSneakEvent(PlayerToggleSneakEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new SneakRecord(event.isSneaking()));
			tryFlush();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerSprintEvent(PlayerToggleSprintEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new SprintRecord(event.isSprinting()));
			tryFlush();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBlockBreak(BlockBreakEvent event)
	{
		if(!mRecording)
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			StoredBlock from,to;
			from = new StoredBlock();
			from.BlockId = event.getBlock().getTypeId();
			from.BlockData = event.getBlock().getData();
			from.BlockLocation = event.getBlock().getLocation();
			
			to = new StoredBlock();
			to.BlockId = 0;
			to.BlockData = 0;
			to.BlockLocation = event.getBlock().getLocation();
			
			mRecords.add(new BlockChangeRecord(from,to));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBlockPlaceEvent(BlockPlaceEvent event)
	{
		if(!mRecording)
			return;
		if(event.isCancelled())
			return;
		if(event.getPlayer() == mPlayerInstance)
		{
			//TODO: Find a way to get what the block was
			StoredBlock from,to;
			from = new StoredBlock();
			from.BlockId = 0;
			from.BlockData = 0;
			from.BlockLocation = event.getBlock().getLocation();
		
			to = new StoredBlock();
			to.BlockId = event.getBlockPlaced().getTypeId();
			to.BlockData = event.getBlockPlaced().getData();
			to.BlockLocation = event.getBlockPlaced().getLocation();

			mRecords.add(new BlockChangeRecord(from,to));

			// Record the inventory change
			ItemStack item = event.getItemInHand().clone();
			item.setAmount(item.getAmount()-1);
			
			if(item.getAmount() == 0)
				item = null;
			
			LogUtil.finest("Inventory Update");
			recordInventoryChange(new UpdateInventoryRecord(mPlayerInstance.getInventory().getHeldItemSlot(), item));
			tryFlush();
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onHeldItemChangeEvent(PlayerItemHeldEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new HeldItemChangeRecord(event.getNewSlot()));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true )
	private void onInventoryClickEvent(InventoryClickEvent event)
	{
		if(!mRecording)
			return;

		if(mPlayerInstance == null)
			return;
		
		Inventory clicked;
		// Find the clicked inv
		if(event.getRawSlot() < event.getView().getTopInventory().getSize())
			clicked = event.getView().getTopInventory();
		else
			clicked = event.getView().getBottomInventory();
		
		if((clicked.getType() != InventoryType.PLAYER || clicked.getHolder() != mPlayerInstance) && !event.isShiftClick())
			return;

		// This event doesnt do anything useful in creative mode, so do full inventory updates. But we cant do it yet, since it has not been applied yet.
		scheduleInvUpdate();
	}
	@EventHandler(priority = EventPriority.LOWEST)
	private void onVehicleEnter(VehicleEnterEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getEntered() == mPlayerInstance)
		{
			mRecords.add(new VehicleMountRecord(true,event.getVehicle()));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST)
	private void onVehicleExit(VehicleExitEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getExited() == mPlayerInstance)
		{
			mRecords.add(new VehicleMountRecord(false,event.getVehicle()));
			tryFlush();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void onVehicleCreate(VehicleCreateEvent event)
	{
		if(!mRecording)
			return;

		// TODO: Vehicle place
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void onVehicleDamage(VehicleDamageEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getAttacker() == mPlayerInstance)
		{
			mRecords.add(new AttackRecord(event.getVehicle(), event.getDamage()));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST)
	private void onVehicleDestroy(VehicleDestroyEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getAttacker() == mPlayerInstance)
		{
			mRecords.add(new AttackRecord(event.getVehicle(), Integer.MAX_VALUE));
			tryFlush();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBucketFill(PlayerBucketFillEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getPlayer() == mPlayerInstance)
		{
			recordInventoryChange(new UpdateInventoryRecord(event.getPlayer().getInventory().getHeldItemSlot(), event.getItemStack()));
			StoredBlock air = new StoredBlock();
			air.BlockId = 0;
			air.BlockData = 0;
			air.BlockLocation = event.getBlockClicked().getLocation();
			mRecords.add(new BlockChangeRecord(new StoredBlock(event.getBlockClicked()), air));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBucketEmpty(PlayerBucketEmptyEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getPlayer() == mPlayerInstance)
		{
			recordInventoryChange(new UpdateInventoryRecord(event.getPlayer().getInventory().getHeldItemSlot(), event.getItemStack()));
			StoredBlock air = new StoredBlock();
			air.BlockId = 0;
			air.BlockData = 0;
			air.BlockLocation = event.getBlockClicked().getLocation();
			mRecords.add(new BlockChangeRecord(air, new StoredBlock(event.getBlockClicked())));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerEnterBed(PlayerBedEnterEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new SleepRecord(true, event.getBed().getLocation()));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerLeaveBed(PlayerBedLeaveEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new SleepRecord(false, event.getPlayer().getLocation()));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlacePainting(PaintingPlaceEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new PaintingChangeRecord(event.getPainting(), true));
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBreakPainting(PaintingBreakByEntityEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getRemover() == mPlayerInstance)
		{
			mRecords.add(new PaintingChangeRecord(event.getPainting(), false));
			tryFlush();
		}
		else if(event.getRemover() instanceof Projectile)
		{
			if(((Projectile)event.getRemover()).getShooter() == mPlayerInstance)
			{
				mRecords.add(new PaintingChangeRecord(event.getPainting(), false));
				tryFlush();
			}
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onItemBreak(PlayerItemBreakEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getPlayer() == mPlayerInstance)
		{
			scheduleInvUpdate();
			tryFlush();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onShear(PlayerShearEntityEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new RightClickActionRecord(au.com.mineauz.PlayerSpy.Records.RightClickActionRecord.Action.Shears, event.getPlayer().getItemInHand(), event.getEntity()));
			scheduleInvUpdate();
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onFish(PlayerFishEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getPlayer() == mPlayerInstance)
		{
			if(event.getState() == State.FISHING)
			{
				// Cast out
				mRecords.add(new RightClickActionRecord(au.com.mineauz.PlayerSpy.Records.RightClickActionRecord.Action.FishCast, null, null));
				scheduleInvUpdate();
			}
			else if(event.getState() == State.CAUGHT_ENTITY)
			{
				mRecords.add(new RightClickActionRecord(au.com.mineauz.PlayerSpy.Records.RightClickActionRecord.Action.FishPullback, null, event.getCaught()));
			}
			else if(event.getState() == State.CAUGHT_FISH)
			{
				mRecords.add(new RightClickActionRecord(au.com.mineauz.PlayerSpy.Records.RightClickActionRecord.Action.FishPullback, ((Item)event.getCaught()).getItemStack(), null));
			}
			else
			{
				mRecords.add(new RightClickActionRecord(au.com.mineauz.PlayerSpy.Records.RightClickActionRecord.Action.FishPullback, null, null));
			}
			
			tryFlush();
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onEggThrow(PlayerEggThrowEvent event)
	{
		if(!mRecording)
			return;
		
		if(event.getPlayer() == mPlayerInstance)
		{
			mRecords.add(new RightClickActionRecord(au.com.mineauz.PlayerSpy.Records.RightClickActionRecord.Action.ProjectileFire, new ItemStack(Material.EGG), null));
			scheduleInvUpdate();
			tryFlush();
		}
	}
	private void startMonitoring()
	{
		LogUtil.fine("Begun recording " + mPlayerName);
		mRecording = true;
		mMonitorId = mPlugin.getServer().getScheduler().scheduleSyncRepeatingTask(mPlugin, new Runnable()
		{
			public void run() 
			{
				if(mPlayerInstance != null)
				{
					if(mLastPlayerLocation.getWorld() != mPlayerInstance.getLocation().getWorld())
					{
						mRecords.add(new WorldChangeRecord(mPlayerInstance.getLocation().getWorld()));
						mRecords.add(new MoveRecord(mPlayerInstance.getLocation(),mPlayerInstance.getEyeLocation()));
						mLastPlayerLocation = mPlayerInstance.getLocation().clone();
						mLastPlayerHeadLocation = mPlayerInstance.getEyeLocation().clone();
						tryFlush();
					}
					//mPlayerInstance.isSneaking()
					// See if they have moved enough
					else if(mPlayerInstance.getLocation().distance(mLastPlayerLocation) > 0.2)
					{
						mRecords.add(new MoveRecord(mPlayerInstance.getLocation(),mPlayerInstance.getEyeLocation()));
						mLastPlayerLocation = mPlayerInstance.getLocation().clone();
						mLastPlayerHeadLocation = mPlayerInstance.getEyeLocation().clone();
						tryFlush();
					}
					// They have changed their direction
					else if(Math.abs(mPlayerInstance.getLocation().getYaw() - mLastPlayerLocation.getYaw()) > 0.5 || Math.abs(mPlayerInstance.getLocation().getPitch() - mLastPlayerLocation.getPitch()) > 0.5)
					{
						mRecords.add(new MoveRecord(mPlayerInstance.getLocation(),mPlayerInstance.getEyeLocation()));
						mLastPlayerLocation = mPlayerInstance.getLocation().clone();
						mLastPlayerHeadLocation = mPlayerInstance.getEyeLocation().clone();
						tryFlush();
					}
					else if(Math.abs(mPlayerInstance.getEyeLocation().getYaw() - mLastPlayerHeadLocation.getYaw()) > 0.5 || Math.abs(mPlayerInstance.getEyeLocation().getPitch() - mLastPlayerHeadLocation.getPitch()) > 0.5)
					{
						mRecords.add(new MoveRecord(mPlayerInstance.getLocation(),mPlayerInstance.getEyeLocation()));
						mLastPlayerLocation = mPlayerInstance.getLocation().clone();
						mLastPlayerHeadLocation = mPlayerInstance.getEyeLocation().clone();
						tryFlush();
					}
				}
				
			}
		}, 1, 1);
	}
	
	private void tryFlush()
	{
		if(mRecords.size() > 100)
			flushData();
	}
	private void flushData()
	{
		LogUtil.finest("Flushing data, time:" + Calendar.getInstance().getTimeInMillis());
		RecordList data = (RecordList)mRecords.clone();
		mLogFile.appendRecordsAsync(data);
		
		mRecords.clear();
	}
	/**
	 * Schedules a full inventory update, this is to combat not being able to track all changes
	 */
	private void scheduleInvUpdate()
	{
		if(mInvScheduleId == -1)
		{
			mInvScheduleId = mPlugin.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
				
				@Override
				public void run() 
				{
					recordInventoryChange(new InventoryRecord(mPlayerInstance.getInventory()));
					
					mInvScheduleId = -1;
				}
			},1L);
		}
	}
	
	private void recordInventoryChange(UpdateInventoryRecord update)
	{
		if(mLastInventory == null)
		{
			LogUtil.warning("ERROR: You are calling recordInventoryChange with an update record without first calling it with a full record");
			return;
		}
		
		if(update.getSlotId() >= mLastInventory.getItems().length)
		{
			// Armour update
			mLastInventory.getArmour()[update.getSlotId() - mLastInventory.getItems().length] = update.getItem();
		}
		else
		{
			// Item update
			mLastInventory.getItems()[update.getSlotId()] = update.getItem();
		}
		LogUtil.finest("Inventory Update. Slot: " + update.getSlotId() + " Item: " + (update.getItem() == null ? "empty" : update.getItem().getType().toString() + ":" + update.getItem().getDurability() + " C:" + update.getItem().getAmount()));
		mRecords.add(update);
	}
	private void recordInventoryChange(InventoryRecord newInv)
	{
		if(mLastInventory != null)
		{
			// Find what changed
			int changedIndex = -1;
			boolean full = false;
			// Check Items
			for(int i = 0; i < newInv.getItems().length; i++)
			{
				boolean changed = false;
				ItemStack oldStack = mLastInventory.getItems()[i];
				ItemStack newStack = newInv.getItems()[i];
				if((oldStack == null && newStack != null) ||
				   (oldStack != null && newStack == null))
					changed = true;
				else if(oldStack != null && newStack != null)
				{
					if(!oldStack.equals(newStack))
						changed = true;
				}
				
				if(changed)
				{
					if(changedIndex != -1)
					{
						full = true;
						break;
					}
					else
						changedIndex = i;
				}
			}
			
			if(!full)
			{
				// Check armour
				for(int i = 0; i < newInv.getArmour().length; i++)
				{
					boolean changed = false;
					ItemStack oldStack = mLastInventory.getArmour()[i];
					ItemStack newStack = newInv.getArmour()[i];
					if((oldStack == null && newStack != null) ||
					   (oldStack != null && newStack == null))
						changed = true;
					else if(oldStack != null && newStack != null)
					{
						if(!oldStack.equals(newStack))
							changed = true;
					}
					
					if(changed)
					{
						if(changedIndex != -1)
						{
							full = true;
							break;
						}
						else
							changedIndex = i + newInv.getItems().length;
					}
				}
			}

			if(full)
			{
				LogUtil.finest("Full Inventory Update");
				mRecords.add(newInv);
			}
			else if(changedIndex != -1)
			{
				UpdateInventoryRecord record = new UpdateInventoryRecord(changedIndex, (changedIndex < newInv.getItems().length ? newInv.getItems()[changedIndex] : newInv.getArmour()[changedIndex - newInv.getItems().length]));
				LogUtil.finest("Inventory Update. Slot: " + record.getSlotId() + " Item: " + (record.getItem() == null ? "empty" : record.getItem().getType().toString() + ":" + record.getItem().getDurability() + " C:" + record.getItem().getAmount()));
				mRecords.add(record);
				
				// Check the hand index
				if(mLastInventory.getHeldSlot() != newInv.getHeldSlot())
				{
					LogUtil.finest("Held Slot Change: " + newInv.getHeldSlot());
					mRecords.add(new HeldItemChangeRecord(newInv.getHeldSlot()));
				}
			}
			else
			{
				LogUtil.finest("Inventory Update. No Change Detected");
			}
		}
		else
		{
			LogUtil.finest("Full Inventory Update");
			mRecords.add(newInv);
		}
		
		mLastInventory = newInv;
	}
	private boolean mRecording;
	private String mPlayerName;
	private Player mPlayerInstance = null;
	private int mMonitorId = -1;
	private int mInvScheduleId = -1;
	private Location mLastPlayerLocation;
	private Location mLastPlayerHeadLocation;
	private InventoryRecord mLastInventory;
	private LogFile mLogFile = null;
	
	// The task id of the kill task used to stop recording after a period of inactivity
	private int mKillTaskId = -1;
	
	private SpyPlugin mPlugin;
	private RecordList mRecords = new RecordList();
}
