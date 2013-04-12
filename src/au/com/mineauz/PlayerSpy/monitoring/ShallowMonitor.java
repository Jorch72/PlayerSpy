package au.com.mineauz.PlayerSpy.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.*;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.debugging.Debug;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;
import au.com.mineauz.PlayerSpy.tracdata.LogFileRegistry;

public class ShallowMonitor 
{
	private LogFile mLog;
	private OfflinePlayer mPlayer;
	private HashMap<String, RecordList> mBuffers = new HashMap<String, RecordList>();
	
	private Inventory mCurrentTransactionInventory;
	// Used for enderchests
	private Location mCurrentTransactionInventoryLocation;
	private ArrayList<ItemStack> mCurrentTransactions;
	
	/**
	 * The size in bytes at which the buffer will be appended to the log
	 */
	public static int sBufferFlushThreshold = 16384;
	
	public ShallowMonitor(OfflinePlayer player)
	{
		mPlayer = player;
		mLog = LogFileRegistry.getLogFileDelayLoad(player);
		if(mLog == null)
			mLog = LogFileRegistry.createLogFile(player);

		if(mLog == null)
			throw new ExceptionInInitializerError(player.getName() + " has no log and it cannot be created.");
		
		mBuffers.put(null, new RecordList());
		
		logRecord(new SessionInfoRecord(this instanceof DeepMonitor));
	}
	@SuppressWarnings("unchecked")
	public ShallowMonitor(ShallowMonitor other)
	{
		mPlayer = other.mPlayer;
		mLog = other.mLog;
		mBuffers = (HashMap<String, RecordList>)other.mBuffers.clone();
	}
	/**
	 * Cleans up everything, appends any remaining data, closes log.
	 */
	public synchronized void shutdown()
	{
		flushAll();
		LogFileRegistry.unloadLogFile(mPlayer);
	}
	
	public OfflinePlayer getMonitorTarget()
	{
		return mPlayer;
	}
	/**
	 * Adds a record to the buffer to be written to the log
	 */
	public synchronized void logRecord(Record record)
	{
		mBuffers.get(null).add(record);
		tryFlush(null);
	}
	public synchronized void logRecord(Record record, String cause)
	{
		if(!mBuffers.containsKey(cause))
			mBuffers.put(cause, new RecordList());
		
		mBuffers.get(cause).add(record);
		tryFlush(cause);
	}
	
	/**
	 * Appends the buffer to the log file if the buffer is large enough
	 */
	private void tryFlush(String cause)
	{
		if(mBuffers.get(cause).getDataSize(cause != null) >= sBufferFlushThreshold)
			flush(cause);
	}
	/**
	 * Appends the buffer to the log file
	 */
	private void flush(String cause)
	{
		if(!mLog.isLoaded())
			return; // Cant flush if logs not loaded :S
		
		RecordList buffer = mBuffers.get(cause);
		if(buffer.size() > 0)
		{
			if(cause != null)
			{
				//LogUtil.info("Appending data to " + mLog.getName() + " using " + cause);
				mLog.appendRecordsAsync((RecordList)buffer.clone(), cause);
			}
			else
				mLog.appendRecordsAsync((RecordList)buffer.clone());
				
			buffer.clear();
		}
	}
	private void flushAll()
	{
		if(!mLog.isLoaded())
			return; // Cant flush if logs not loaded :S
		
		for(Entry<String, RecordList> ent : mBuffers.entrySet())
		{
			if(ent.getValue().size() == 0)
				continue;
			
			if(ent.getKey() != null)
			{
				//LogUtil.info("Appending data to " + mLog.getName() + " using " + ent.getKey());
				mLog.appendRecordsAsync((RecordList)ent.getValue().clone(), ent.getKey());
			}
			else
				mLog.appendRecordsAsync((RecordList)ent.getValue().clone());

			ent.getValue().clear();
		}
	}
	
	/**
	 * Gets all current block change records in the buffers
	 */
	public synchronized List<Pair<String, RecordList>> getBufferedRecords()
	{
		ArrayList<Pair<String,RecordList>> results = new ArrayList<Pair<String,RecordList>>();
		for(Entry<String, RecordList> buffer : mBuffers.entrySet())
		{
			RecordList output = (RecordList)buffer.getValue().clone();
			
			if(!output.isEmpty())
				results.add(new Pair<String, RecordList>(buffer.getKey(), output));
		}
		
		return results;
	}
	
	public void beginTransaction(Inventory inventory, Location enderChestLocation)
	{
		if(mCurrentTransactionInventory != null)
			endTransaction();
		
		mCurrentTransactionInventory = inventory;
		mCurrentTransactions = new ArrayList<ItemStack>();
		mCurrentTransactionInventoryLocation = enderChestLocation;
		
		Debug.finer("Beginning Transaction");
	}
	public void doTransaction(ItemStack item, boolean take)
	{
		Debug.loggedAssert(mCurrentTransactionInventory != null);
		
		// Total up transactions
		for(ItemStack transaction : mCurrentTransactions)
		{
			if(Utility.areEqualIgnoreAmount(transaction, item))
			{
				// Total it up
				if(take)
					transaction.setAmount(transaction.getAmount() - item.getAmount());
				else
					transaction.setAmount(transaction.getAmount() + item.getAmount());
				
				return;
			}
		}
		
		// No match
		ItemStack transaction = item.clone();
		if(take)
			transaction.setAmount(-transaction.getAmount());
		mCurrentTransactions.add(transaction);
	}
	public void endTransaction()
	{
		Debug.loggedAssert(mCurrentTransactionInventory != null);
		// Log what ever is left
		for(ItemStack transaction : mCurrentTransactions)
		{
			if(transaction.getAmount() != 0)
			{
				InventoryTransactionRecord record = null;
				
				if(transaction.getAmount() < 0)
				{
					transaction.setAmount(-transaction.getAmount());
					record = InventoryTransactionRecord.newTakeFromInventory(transaction, mCurrentTransactionInventory, mCurrentTransactionInventoryLocation);
				}
				else
					record = InventoryTransactionRecord.newAddToInventory(transaction, mCurrentTransactionInventory, mCurrentTransactionInventoryLocation);
				
				Debug.finer(record.toString());
				logRecord(record);
			}
		}
		
		Debug.finer("Ended Transaction");
		mCurrentTransactionInventory = null;
		mCurrentTransactionInventoryLocation = null;
		mCurrentTransactions.clear();
		mCurrentTransactions = null;
	}
	
	//***************************
	// Events
	//***************************
	
	
	public void onPlayerChatEvent(AsyncPlayerChatEvent event)
	{
		logRecord(new ChatCommandRecord(event.getMessage(), event.isCancelled()));
	}
	public void onPlayerCommandEvent(PlayerCommandPreprocessEvent event)
	{
		logRecord(new ChatCommandRecord(event.getMessage(), event.isCancelled()));
	}

	public void onPlayerChangedWorld(PlayerChangedWorldEvent event)
	{
		logRecord(new WorldChangeRecord(event.getPlayer().getWorld()));
	}
	
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
	{
		logRecord(new GameModeRecord(event.getNewGameMode().getValue()));
	}
	
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		logRecord(new InteractRecord(event.getAction(), event.getClickedBlock(), event.getItem(), null));
	}
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
	{
		logRecord(new InteractRecord(Action.RIGHT_CLICK_AIR,null, null, event.getRightClicked()));
	}
	
	public void onPlayerShearEntity(PlayerShearEntityEvent event)
	{
		logRecord(new RightClickActionRecord(RightClickActionRecord.Action.Shears, event.getPlayer().getItemInHand(), event.getEntity()));
	}
	
	
	
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		logRecord(new DeathRecord(event.getEntity().getLocation(),event.getDeathMessage()));
	}
	
	public void onDamage(Entity damageEnt, Block damageBlock, int damageAmount)
	{
		if(damageEnt != null)
		{
			if(damageEnt instanceof Projectile)
			{
				// We are being damaged
				Debug.finest("Target was shot by " + ((Projectile)damageEnt).getShooter().getType().toString());
				logRecord(new DamageRecord(((Projectile)damageEnt).getShooter(),damageAmount));
			}
			else
			{
				// We are being damaged
				Debug.finest("Target was damaged by entity");
				logRecord(new DamageRecord(damageEnt,damageAmount));
			}
		}
		else if(damageBlock != null)
		{
			// TODO: Include blocks as a damage source
			logRecord(new DamageRecord(null, damageAmount));
		}
		else
		{
			logRecord(new DamageRecord(null, damageAmount));
		}
	}
	public void onAttack(Entity target, int damageAmount)
	{
		logRecord(new AttackRecord(target,damageAmount));
	}
	public void onBlockPlace(Block block, BlockState replaced)
	{
		logRecord(new BlockChangeRecord(replaced, block.getState(), true));
	}
	public void onBlockBreak(Block block)
	{
		logRecord(new BlockChangeRecord(block.getState(), null, false));
	}
	public void onBucketFill(Block block, ItemStack resultant)
	{
		if(block != null)
			logRecord(new BlockChangeRecord(block.getState(), null, false));
	}
	public void onBucketEmpty(Block block, ItemStack resultant)
	{
		logRecord(new BlockChangeRecord(null, block.getState(), true));
	}
	public void onItemDrop(ItemStack item)
	{
		logRecord(new DropItemRecord(item));
	}
	public void onItemPickup(Item item) 
	{
		logRecord(new ItemPickupRecord(item));
	}
	public void onEggThrow() 
	{
		logRecord(new RightClickActionRecord(RightClickActionRecord.Action.ProjectileFire, new ItemStack(Material.EGG), null));
	}
	
	public void onPlaceHanging(HangingPlaceEvent event)
	{	
		if(event.getEntity() instanceof Painting)
			logRecord(new PaintingChangeRecord(((Painting)event.getEntity()), true));
		else if(event.getEntity() instanceof ItemFrame)
			logRecord(new ItemFrameChangeRecord(((ItemFrame)event.getEntity()), true));
			
	}
	public void onBreakHanging(HangingBreakByEntityEvent event)
	{	
		if(event.getEntity() instanceof Painting)
			logRecord(new PaintingChangeRecord(((Painting)event.getEntity()), false));
		else if(event.getEntity() instanceof ItemFrame)
			logRecord(new ItemFrameChangeRecord(((ItemFrame)event.getEntity()), false));
	}
	
	public void onConsume(ItemStack item)
	{
		logRecord(new RightClickActionRecord(au.com.mineauz.PlayerSpy.Records.RightClickActionRecord.Action.Eat, item, null));
		
	}
}
