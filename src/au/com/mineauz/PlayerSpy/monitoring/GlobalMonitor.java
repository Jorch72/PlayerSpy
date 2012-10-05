package au.com.mineauz.PlayerSpy.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.server.EntityLiving;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.material.MaterialData;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.Pair;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Utility;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.LoginRecord;
import au.com.mineauz.PlayerSpy.Records.LogoffRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.SessionInfoRecord;
import au.com.mineauz.PlayerSpy.Records.LogoffRecord.LogoffType;

public class GlobalMonitor implements Listener
{
	private HashMap<OfflinePlayer, ShallowMonitor> mShallowLogs;
	private HashMap<OfflinePlayer, DeepMonitor> mDeepLogs;
	private LogFile mGlobalLog;
	private ItemFlowTracker mItemTracker;
	public static final GlobalMonitor instance = new GlobalMonitor();

	private HashMap<String, RecordList> mBuffers = new HashMap<String, RecordList>();
	private HashMap<Cause, Pair<RecordList,Cause>> mPendingRecords = new HashMap<Cause, Pair<RecordList,Cause>>();

	private SpreadTracker mSpreadTracker = new SpreadTracker();
	private CauseFinder mCauseFinder = new CauseFinder();
	
	private GlobalMonitor()
	{
		mShallowLogs = new HashMap<OfflinePlayer, ShallowMonitor>();
		mDeepLogs = new HashMap<OfflinePlayer, DeepMonitor>();
		
	}
	
	public void initialize()
	{
		LogUtil.finer("Staring global monitor");
	
		for(Player player : Bukkit.getOnlinePlayers())
			attachShallow(player);
		
		mGlobalLog = LogFileRegistry.getGlobalLog();
		if(mGlobalLog == null)
			mGlobalLog = LogFileRegistry.createGlobalLog();
		
		if(mGlobalLog == null)
			LogUtil.severe("Unable to create global log file");
		
		mItemTracker = new ItemFlowTracker();
		Bukkit.getPluginManager().registerEvents(this, SpyPlugin.getInstance());
		Bukkit.getPluginManager().registerEvents(mItemTracker, SpyPlugin.getInstance());
	}
	public void shutdown() 
	{
		LogUtil.fine("Shutting down the Global Monitor");
		flushAll();
		if(mGlobalLog != null)
		{
			mGlobalLog.close();
			if(!mGlobalLog.isLoaded())
				LogFileRegistry.unloadGlobalLogFile();
			mGlobalLog = null;
		}
		
		for(DeepMonitor monitor : mDeepLogs.values())
			monitor.shutdown();
		
		for(ShallowMonitor monitor : mShallowLogs.values())
			monitor.shutdown();
		
		mDeepLogs.clear();
		mShallowLogs.clear();
	}
	public void update()
	{
		mSpreadTracker.doUpdate();
		mCauseFinder.update();
	}
	private void attachShallow(Player player)
	{
		LogUtil.finer("Attaching shallow monitor to " + player.getName());
		mShallowLogs.put(player, new ShallowMonitor(player));
	}
	private void removeShallow(Player player)
	{
		if(!mShallowLogs.containsKey(player))
			return;
		
		ShallowMonitor mon = mShallowLogs.remove(player);
		mon.shutdown();
	}
	
	public void attachDeep(OfflinePlayer player)
	{
		try
		{
			DeepMonitor monitor = null;
			if(mShallowLogs.containsKey(player))
			{
				monitor = new DeepMonitor(mShallowLogs.remove(player));
			}
			else
			{
				monitor = new DeepMonitor(player);
			}
			
			LogUtil.fine("Attaching Deep Monitor to " + player.getName());
			monitor.logRecord(new SessionInfoRecord(true));
			mDeepLogs.put(player, monitor);
		}
		catch(ExceptionInInitializerError e)
		{
			LogUtil.severe(e.getMessage());
		}
	}
	public void removeDeep(OfflinePlayer player)
	{
		if(!mDeepLogs.containsKey(player))
			return;
		
		LogUtil.fine("Removing Deep Monitor from " + player.getName());
		DeepMonitor deep = mDeepLogs.remove(player);
		deep.logRecord(new SessionInfoRecord(false));
		
		if(player.isOnline())
			mShallowLogs.put(player, new ShallowMonitor(deep));
		else
		{
			deep.shutdown();
		}
	}
	
	/**
	 * Gets the monitor currently watching player
	 * @return An instance of ShallowMonitor or DeepMonitor for that player, or null if there isnt one
	 */
	public ShallowMonitor getMonitor(OfflinePlayer player)
	{
		ShallowMonitor mon = mShallowLogs.get(player);
		if(mon != null)
			return mon;
		
		return mDeepLogs.get(player);
	}
	/**
	 * Gets the monitor currently watching player. It will only return deep monitors.
	 * @return An instance of DeepMonitor for that player, or null if there isnt one
	 */
	public DeepMonitor getDeepMonitor(OfflinePlayer player)
	{
		return mDeepLogs.get(player);
	}
	public List<ShallowMonitor> getAllMonitors()
	{
		ArrayList<ShallowMonitor> monitors = new ArrayList<ShallowMonitor>();
		monitors.addAll(mShallowLogs.values());
		monitors.addAll(mDeepLogs.values());
		return monitors;
	}
	/**
	 * Logs a record with a non player process
	 * @param cause The name of the process starting with #
	 * @param record The record to log
	 */
	public void logRecordGlobal(String cause, Record record)
	{
		if(!mBuffers.containsKey(cause))
			mBuffers.put(cause, new RecordList());
		
		mBuffers.get(cause).add(record);
		
		tryFlush(cause);
	}
	private void tryFlush(String cause)
	{
		if(mBuffers.get(cause).getDataSize() >= ShallowMonitor.sBufferFlushThreshold)
			flush(cause);
	}
	private void flush(String cause)
	{
		if(mBuffers.get(cause).size() > 0)
		{
			mGlobalLog.appendRecordsAsync((RecordList)mBuffers.get(cause).clone(), cause);
			mBuffers.get(cause).clear();
		}
	}
	
	private void flushAll()
	{
		for(Entry<String, RecordList> ent : mBuffers.entrySet())
		{
			if(ent.getValue().size() == 0)
				continue;
			
			mGlobalLog.appendRecordsAsync((RecordList)ent.getValue().clone(), ent.getKey());
			ent.getValue().clear();
		}
	}
	
	/**
	 * Sends the records off to be logged by the appropriate logger, or to be sidelined until a cause is found
	 * @param records The records to log, cannot be null
	 * @param cause The cause of the records, cannot be unknown or null
	 * @param defaultCause The backup cause to log against. Used only when the cause was a placeholder, and the result came back as unknown. Can be null if cause is not a placeholder
	 */
	public void logRecords(RecordList records, Cause cause, Cause defaultCause)
	{
		assert records != null;
		assert cause != null && !cause.isUnknown();
		assert defaultCause != null || !cause.isPlaceholder();
		
		if(cause.isPlaceholder())
		{
			// Put the records aside until the cause is found
			if(mPendingRecords.containsKey(cause))
			{
				// Add onto the existing one
				mPendingRecords.get(cause).getArg1().addAll(records);
			}
			else
				// Add a new one
				mPendingRecords.put(cause, new Pair<RecordList, Cause>(records, defaultCause));
		}
		else
		{
			// Find where to log it
			if(cause.isGlobal())
			{
				if(!mBuffers.containsKey(cause.getExtraCause()))
					mBuffers.put(cause.getExtraCause(), new RecordList());
				
				mBuffers.get(cause.getExtraCause()).addAll(records);
				
				tryFlush(cause.getExtraCause());
			}
			else
			{
				ShallowMonitor mon = getMonitor(cause.getCausingPlayer());
				if(mon != null)
				{
					for(Record record : records)
						mon.logRecord(record, cause.getExtraCause());
				}
				else
				{
					// TODO: Offline monitor
				}
			}
		}
		
	}
	
	public void logRecord(Record record, Cause cause, Cause defaultCause)
	{
		RecordList records = new RecordList();
		records.add(record);
		logRecords(records, cause, defaultCause);
	}
	
	public void delayLogBlockChange(final Block block, final Cause cause, final Cause backupCause)
	{
		final MaterialData material = block.getState().getData().clone();
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(SpyPlugin.getInstance(), new Runnable() {
			
			@Override
			public void run() 
			{
				MaterialData newMaterial = block.getLocation().getBlock().getState().getData().clone();
				if(material.equals(newMaterial))
					return;
				BlockChangeRecord record = new BlockChangeRecord(material, newMaterial, block.getLocation(), (material.getItemType() == Material.AIR ? true : false));
				logRecord(record, cause, backupCause);
			}
		});
	}
	
	//***********************
	// Event Handlers
	//***********************
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		if(!mDeepLogs.containsKey(event.getPlayer()))
			attachShallow(event.getPlayer());
		
		ShallowMonitor mon = getMonitor(event.getPlayer());
		if(mon != null)
			mon.logRecord(new LoginRecord(event.getPlayer().getLocation()));
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerQuit(PlayerQuitEvent event)
	{
		ShallowMonitor mon = getMonitor(event.getPlayer());
		
		if(mon != null)
			mon.logRecord(new LogoffRecord(LogoffType.Quit, event.getQuitMessage()));
		removeShallow(event.getPlayer());
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerKicked(PlayerKickEvent event)
	{
		if(event.getPlayer().isBanned())
			getMonitor(event.getPlayer()).logRecord(new LogoffRecord(LogoffType.Ban, event.getReason()));
		else
			getMonitor(event.getPlayer()).logRecord(new LogoffRecord(LogoffType.Kick, event.getReason()));
		
		removeShallow(event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerChatEvent(AsyncPlayerChatEvent event)
	{
		ShallowMonitor mon = getMonitor(event.getPlayer());
		
		if(mon != null)
			mon.onPlayerChatEvent(event);
	}
	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerCommandEvent(PlayerCommandPreprocessEvent event)
	{
		ShallowMonitor mon = getMonitor(event.getPlayer());
		
		if(mon != null)
			mon.onPlayerCommandEvent(event);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerChangedWorld(PlayerChangedWorldEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
		ShallowMonitor mon = getMonitor(event.getPlayer());
		
		if(mon != null)
			mon.onPlayerChangedWorld(event);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
		ShallowMonitor mon = getMonitor(event.getPlayer());
		
		if(mon != null)
			mon.onPlayerGameModeChange(event);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerInteract(PlayerInteractEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
		ShallowMonitor mon = getMonitor(event.getPlayer());
		
		if(mon != null)
			mon.onPlayerInteract(event);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerInteractEntity(PlayerInteractEntityEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
		ShallowMonitor mon = getMonitor(event.getPlayer());
		
		if(mon != null)
			mon.onPlayerInteractEntity(event);
	}
	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerRespawn(PlayerRespawnEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
		ShallowMonitor mon = getMonitor(event.getPlayer());
		
		if(mon != null)
			mon.onPlayerRespawn(event);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerShearEntity(PlayerShearEntityEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
		ShallowMonitor mon = getMonitor(event.getPlayer());
		
		if(mon != null)
			mon.onPlayerShearEntity(event);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerFish(PlayerFishEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
		ShallowMonitor mon = getMonitor(event.getPlayer());
		
		if(mon != null)
			mon.onPlayerFish(event);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onPlayerDeath(PlayerDeathEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getEntity().getInventory());
		ShallowMonitor mon = getMonitor(event.getEntity());
		
		if(mon != null)
			mon.onPlayerDeath(event);
	}
	@EventHandler(ignoreCancelled = true)
	private void onPlayerMove(PlayerMoveEvent event)
	{
		DeepMonitor mon = mDeepLogs.get(event.getPlayer());
		if(mon != null)
			mon.onMove(event.getPlayer().getLocation(), event.getPlayer().getEyeLocation());
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onDamageByEntity(EntityDamageByEntityEvent event)
	{
		if(event.getEntity() instanceof Player)
		{
			ShallowMonitor mon = getMonitor((Player)event.getEntity());
		
			if(mon != null)
				mon.onDamage(event.getDamager(), null, event.getDamage());
			
			if(event.getDamager() instanceof Player)
			{
				mon = getMonitor((Player)event.getEntity());
				
				if(mon != null)
					mon.onAttack(event.getEntity(), event.getDamage());
			}
			// Record attack through projectile
			else if(event.getDamager() instanceof Projectile && ((Projectile)event.getDamager()).getShooter() instanceof Player)
			{
				mon = getMonitor((Player)((Projectile)event.getDamager()).getShooter());
				
				if(mon != null)
					mon.onAttack(event.getEntity(), event.getDamage());
			}
		}
		else
		{
			if(event.getDamager() != null)
			{
				// Record direct attack
				if(event.getDamager() instanceof Player)
				{
					//ShallowMonitor mon = getMonitor((Player)event.getEntity());
					
					//if(mon != null)
						//mon.onAttack(event.getEntity(), event.getDamage());
				}
				// Record attack through projectile
				else if(event.getDamager() instanceof Projectile)
				{
					//ShallowMonitor mon = getMonitor((Player)((Projectile)event.getDamager()).getShooter());
					
					//if(mon != null)
						//mon.onAttack(event.getEntity(), event.getDamage());
				}
			}
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onDamageByBlock(EntityDamageByBlockEvent event)
	{
		if(event.getEntity() instanceof Player)
		{
			ShallowMonitor mon = getMonitor((Player)event.getEntity());
			
			if(mon != null)
				mon.onDamage(null, event.getDamager(), event.getDamage());
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onDamage(EntityDamageEvent event)
	{
		if(event.getEntity() instanceof Player)
		{
			if(event.getCause() == DamageCause.DROWNING ||
			   event.getCause() == DamageCause.FALL ||
			   event.getCause() == DamageCause.FIRE_TICK ||
			   event.getCause() == DamageCause.FIRE ||
			   event.getCause() == DamageCause.POISON ||
			   event.getCause() == DamageCause.STARVATION ||
			   event.getCause() == DamageCause.SUFFOCATION ||
			   event.getCause() == DamageCause.SUICIDE ||
			   event.getCause() == DamageCause.VOID)
			{
				ShallowMonitor mon = getMonitor((Player)event.getEntity());
				
				if(mon != null)
					mon.onDamage(null, null, event.getDamage());
			}
		}
		
			
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onEntityDeath(EntityDeathEvent event)
	{
		ShallowMonitor mon = getMonitor(event.getEntity().getKiller());
		
		if(mon != null)
			mon.onAttack(event.getEntity(), -1);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBlockBreak(BlockBreakEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
		ShallowMonitor monitor = getMonitor(event.getPlayer());
		if(monitor != null)
			monitor.onBlockBreak(event.getBlock());
		
		if((event.getBlock().getType() == Material.BROWN_MUSHROOM || event.getBlock().getType() == Material.RED_MUSHROOM) ||
				(event.getBlock().getType() == Material.GRASS || event.getBlock().getType() == Material.MYCEL) ||
				event.getBlock().getType() == Material.FIRE)
			mSpreadTracker.remove(event.getBlock().getLocation());
		
		if(event.getBlock().getType() == Material.ICE && SpyPlugin.getSettings().recordFluidFlow)
			mSpreadTracker.addSource(event.getBlock().getLocation(), Cause.playerCause(event.getPlayer(), "#water"));
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBlockPlace(BlockPlaceEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
		ShallowMonitor monitor = getMonitor(event.getPlayer());
		if(monitor != null)
			monitor.onBlockPlace(event.getBlock(),event.getBlockReplacedState());
		
		if(event.getBlockPlaced().getType() == Material.FIRE && SpyPlugin.getSettings().recordFireSpread)
			mSpreadTracker.addSource(event.getBlockPlaced().getLocation(), Cause.playerCause(event.getPlayer()));
		if((event.getBlockPlaced().getType() == Material.WATER || event.getBlockPlaced().getType() == Material.LAVA) && SpyPlugin.getSettings().recordFluidFlow)
			mSpreadTracker.addSource(event.getBlockPlaced().getLocation(), Cause.playerCause(event.getPlayer()));
		if((event.getBlockPlaced().getType() == Material.BROWN_MUSHROOM || event.getBlockPlaced().getType() == Material.RED_MUSHROOM) && SpyPlugin.getSettings().recordMushroomSpread)
			mSpreadTracker.addSource(event.getBlockPlaced().getLocation(), Cause.playerCause(event.getPlayer()));
		if((event.getBlockPlaced().getType() == Material.GRASS || event.getBlockPlaced().getType() == Material.MYCEL) && SpyPlugin.getSettings().recordGrassSpread)
			mSpreadTracker.addSource(event.getBlockPlaced().getLocation(), Cause.playerCause(event.getPlayer()));
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBucketFill(PlayerBucketFillEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
		ShallowMonitor monitor = getMonitor(event.getPlayer());
		if(monitor != null)
			monitor.onBucketFill(event.getBlockClicked(), event.getItemStack());
		
		mSpreadTracker.remove(event.getBlockClicked().getLocation());
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBucketEmpty(PlayerBucketEmptyEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
		
		ShallowMonitor monitor = getMonitor(event.getPlayer());
		if(monitor != null && event.getBlockClicked() != null)
			monitor.onBucketEmpty(event.getBlockClicked().getRelative(event.getBlockFace()), event.getItemStack());
		
		if(SpyPlugin.getSettings().recordFluidFlow)
		{
			String cause = "";
			switch(event.getBucket())
			{
			case WATER_BUCKET:
				cause = "#water";
				break;
			case LAVA_BUCKET:
				cause = "#lava";
				break;
			default:
				return;
			}
			if(event.getBlockClicked() == null)
				return;
			
			mSpreadTracker.addSource(event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), Cause.playerCause(event.getPlayer(), cause));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onItemDrop(PlayerDropItemEvent event)
	{
		ShallowMonitor mon = GlobalMonitor.instance.getMonitor(event.getPlayer());
		if(mon != null)
			mon.onItemDrop(event.getItemDrop().getItemStack());
		
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onItemPickup(PlayerPickupItemEvent event)
	{
		ShallowMonitor mon = GlobalMonitor.instance.getMonitor(event.getPlayer());
		if(mon != null)
			mon.onItemPickup(event.getItem());
		
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onItemBreak(PlayerItemBreakEvent event)
	{
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onEggThrow(PlayerEggThrowEvent event)
	{
		ShallowMonitor mon = GlobalMonitor.instance.getMonitor(event.getPlayer());
		if(mon != null)
			mon.onEggThrow();
		mItemTracker.scheduleInventoryUpdate(event.getPlayer().getInventory());
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onEntityFormBlock(EntityBlockFormEvent event)
	{
		String cause = "#" + event.getEntity().getType().getName().toLowerCase();
		
		BlockChangeRecord record = new BlockChangeRecord(null, event.getBlock(), true); 
		logRecordGlobal(cause, record);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onEntityChangeBlock(EntityChangeBlockEvent event)
	{
		String cause = "#" + event.getEntity().getType().getName().toLowerCase();
		BlockChangeRecord record = null;
		if(event.getEntityType() == EntityType.ENDERMAN)
		{
			if(event.getBlock().getType() == Material.AIR)
				record = new BlockChangeRecord(event.getBlock().getState().getData(), ((Enderman)event.getEntity()).getCarriedMaterial(), event.getBlock().getLocation(), true);
			else
				record = new BlockChangeRecord(event.getBlock().getState().getData(), new MaterialData(event.getTo()), event.getBlock().getLocation(), false);
		}
		else
		{
			record = new BlockChangeRecord(event.getBlock().getState().getData(), new MaterialData(event.getTo()), event.getBlock().getLocation(), event.getTo() != Material.AIR);
		}

		logRecordGlobal(cause, record);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onEntityExplode(EntityExplodeEvent event)
	{
		if(event.getEntityType() == EntityType.PRIMED_TNT)
		{
			Location loc = ((TNTPrimed)event.getEntity()).getLocation();
			RecordList records = new RecordList();
			for(Block block : event.blockList())
			{
				BlockChangeRecord record = new BlockChangeRecord(block, null, false);
				records.add(record);
			}
			// TODO: See if we can track the entity back to its creation and search there
			Cause cause = mCauseFinder.getCauseFor(loc);
			Cause defaultCause = Cause.globalCause("#tnt");
			logRecords(records, cause, defaultCause);
		}
		else
		{
			Cause cause = null;
			String extraCause = "#" + event.getEntityType().getName().toLowerCase();
			OfflinePlayer player = null;
			
			// Find who set off the mob
			if(event.getEntity() instanceof Monster && ((Monster)event.getEntity()).getTarget() instanceof Player)
				player = (Player)((Monster)event.getEntity()).getTarget();
			
			if(player != null)
				cause = Cause.playerCause(player, extraCause);
			else
				cause = Cause.globalCause(extraCause);
			
			RecordList records = new RecordList();
			for(Block block : event.blockList())
				records.add(new BlockChangeRecord(block, null, false));

			logRecords(records, cause, null);
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBlockGrow(BlockGrowEvent event)
	{
		
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBlockFade(BlockFadeEvent event)
	{
		Cause cause = null;
		Cause backupCause = null;
		switch(event.getBlock().getType())
		{
		case GRASS:
		case MYCEL:
			cause = Cause.globalCause("#decay");
			
			if(SpyPlugin.getSettings().recordGrassSpread)
				mSpreadTracker.remove(event.getBlock().getLocation());
			break;
		case ICE:
			if(SpyPlugin.getSettings().recordFluidFlow)
			{
				cause = mCauseFinder.getCauseFor(event.getBlock().getLocation());
				backupCause = Cause.globalCause("#melt");
				mSpreadTracker.addSource(event.getBlock().getLocation(), cause);
			}
			else
				cause = Cause.globalCause("#melt");
			break;
		case SNOW:
			cause = Cause.globalCause("#melt");
			break;
		case FIRE:
			cause = Cause.globalCause("#extinguished");
			
			if(SpyPlugin.getSettings().recordFireSpread)
				mSpreadTracker.remove(event.getBlock().getLocation());
			break;
		default:
			return;
		}
		
		BlockChangeRecord record = new BlockChangeRecord(event.getBlock(), event.getNewState().getBlock(), false);
		logRecord(record, cause, backupCause);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBlockBurn(BlockBurnEvent event)
	{
		Cause cause = null;
		// Attempt to find the cause of the burn
		for(BlockFace face : BlockFace.values())
		{
			if(event.getBlock().getRelative(face).getType() == Material.FIRE)
			{
				// See if there was a logged cause
				cause = mSpreadTracker.getCause(event.getBlock().getRelative(face).getLocation());
				if(cause != null)
					break;
			}
		}

		// Make sure there is at least the global #fire cause
		if(cause == null)
			cause = Cause.globalCause("#fire");
		if(cause.getExtraCause() == null)
			cause.update(Cause.playerCause(cause.getCausingPlayer(), "#fire"));
		
		// Log it
		BlockChangeRecord record = new BlockChangeRecord(event.getBlock(),null, false);
		logRecord(record, cause, null);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onLeavesDecay(LeavesDecayEvent event)
	{
		String cause = "#decay";
		BlockChangeRecord record = new BlockChangeRecord(event.getBlock(),null, false);
		logRecordGlobal(cause,record);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBlockFromTo(BlockFromToEvent event)
	{
		if(event.getBlock().getType() != Material.DRAGON_EGG)
		{
			if(SpyPlugin.getSettings().recordFluidFlow)
			{
				Cause cause = null;
				if(!mSpreadTracker.spreadTo(event.getBlock().getLocation(), event.getToBlock().getLocation()))
				{
					// Start finding out who placed it
					cause = mCauseFinder.getCauseFor(event.getBlock().getLocation());
					// Add the source and respread it
					mSpreadTracker.addSource(event.getBlock().getLocation(), cause);
					mSpreadTracker.spreadTo(event.getBlock().getLocation(), event.getToBlock().getLocation());
				}
				else
					cause = mSpreadTracker.getCause(event.getToBlock().getLocation());
				
				Cause backupCause = null;
				if(event.getBlock().getType() == Material.LAVA)
					backupCause = Cause.globalCause("#lava");
				else if(event.getBlock().getType() == Material.WATER)
					backupCause = Cause.globalCause("#water");
				else
					backupCause = Cause.globalCause("#fluid"); // Mods i guess?
				
				delayLogBlockChange(event.getToBlock(), cause, backupCause);
			}
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBlockSpread(BlockSpreadEvent event)
	{
		// Check config
		boolean ok = false;
		if(event.getNewState().getType() == Material.FIRE && SpyPlugin.getSettings().recordFireSpread)
			ok = true;
		else if((event.getNewState().getType() == Material.GRASS || event.getNewState().getType() == Material.MYCEL) && SpyPlugin.getSettings().recordGrassSpread)
			ok = true;
		else if((event.getNewState().getType() == Material.RED_MUSHROOM || event.getNewState().getType() == Material.BROWN_MUSHROOM) && SpyPlugin.getSettings().recordMushroomSpread)
			ok = true;
		
		if(!ok)
			return;

		// Track spread
		Cause cause = null;
		if(!mSpreadTracker.spreadTo(event.getSource().getLocation(), event.getBlock().getLocation()))
		{
			// Start finding out who placed it
			cause = mCauseFinder.getCauseFor(event.getSource().getLocation());
			// Add the source and respread it
			mSpreadTracker.addSource(event.getSource().getLocation(), cause);
			mSpreadTracker.spreadTo(event.getSource().getLocation(), event.getBlock().getLocation());
		}
		else
			cause = mSpreadTracker.getCause(event.getBlock().getLocation());
		
		Cause backupCause = Cause.globalCause("#spread");
		
		BlockChangeRecord record = new BlockChangeRecord(event.getBlock().getState(),event.getNewState(), true);
		logRecord(record, cause, backupCause);
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBlockPhysics(BlockPhysicsEvent event)
	{
		
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onBlockIgite(BlockIgniteEvent event)
	{
		// Flint and steal is already covered by the block place event
		if(event.getCause() == IgniteCause.FLINT_AND_STEEL)
			return;
		
		BlockState after = event.getBlock().getState();
		after.setType(Material.FIRE);
		
		String extraCause = null;
		switch(event.getCause())
		{
		case FIREBALL:
			extraCause += "#fireball";
			break;
		case LAVA:
			extraCause += "#lavafire";
			break;
		case LIGHTNING:
			extraCause += "#lightning";
			break;
		default:
			break;
		}
		
		BlockChangeRecord record = new BlockChangeRecord(null, after, true);
		
		Cause cause = null;
		if(event.getPlayer() != null)
		{
			if(extraCause != null)
				cause = Cause.playerCause(event.getPlayer(), extraCause);
			else
				cause = Cause.playerCause(event.getPlayer());
			
			ShallowMonitor mon = getMonitor(event.getPlayer());
			if(mon != null)
			{
				mon.logRecord(record);
			}
		}
		else
		{
			if(extraCause != null)
			{
				cause = Cause.globalCause(extraCause);
				logRecordGlobal(extraCause, record);
			}
		}
		
		if(cause == null)
			return;
		// Handle keeping track of the fire
		if(event.getCause() != IgniteCause.SPREAD & SpyPlugin.getSettings().recordFireSpread)
		{
			mSpreadTracker.addSource(event.getBlock().getLocation(), cause);
		}
	}
	@EventHandler
	private void onCauseFound(CauseFinder.CauseFoundEvent event)
	{
		// Apply the records
		if(mPendingRecords.containsKey(event.getPlaceholder()))
		{
			Pair<RecordList, Cause> records = mPendingRecords.remove(event.getPlaceholder());
			event.getPlaceholder().update(event.getCause());
			
			// Log the records using the new cause
			if(event.getCause().isUnknown())
				logRecords(records.getArg1(), records.getArg2(), records.getArg2());
			else
				logRecords(records.getArg1(), event.getCause(), records.getArg2());
		}
	}
}
