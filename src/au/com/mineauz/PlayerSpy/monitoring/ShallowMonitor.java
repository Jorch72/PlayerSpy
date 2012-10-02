package au.com.mineauz.PlayerSpy.monitoring;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Utility;
import au.com.mineauz.PlayerSpy.Records.*;

public class ShallowMonitor 
{
	private LogFile mLog;
	private OfflinePlayer mPlayer;
	private RecordList mBuffer;
	
	private Inventory mCurrentTransactionInventory;
	private ArrayList<ItemStack> mCurrentTransactions;
	
	/**
	 * The size in bytes at which the buffer will be appended to the log
	 */
	public static int sBufferFlushThreshold = 4096;
	
	public ShallowMonitor(OfflinePlayer player)
	{
		mPlayer = player;
		mLog = LogFileRegistry.getLogFile(player);
		if(mLog == null)
			mLog = LogFileRegistry.createLogFile(player);

		if(mLog == null)
			throw new ExceptionInInitializerError(player.getName() + " has no log and it cannot be created.");
		
		mBuffer = new RecordList();
	}
	public ShallowMonitor(ShallowMonitor other)
	{
		mPlayer = other.mPlayer;
		mLog = other.mLog;
		mBuffer = (RecordList)other.mBuffer.clone();
	}
	/**
	 * Cleans up everything, appends any remaining data, closes log.
	 */
	public synchronized void shutdown()
	{
		flush();
		mLog.closeAsync();
		if(!mLog.isLoaded())
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
		mBuffer.add(record);
		tryFlush();
	}
	
	/**
	 * Appends the buffer to the log file if the buffer is large enough
	 */
	private void tryFlush()
	{
		if(mBuffer.getDataSize() >= sBufferFlushThreshold)
			flush();
	}
	/**
	 * Appends the buffer to the log file
	 */
	private void flush()
	{
		if(mBuffer.size() > 0)
		{
			mLog.appendRecordsAsync((RecordList)mBuffer.clone());
			mBuffer.clear();
		}
	}
	
	public void beginTransaction(Inventory inventory)
	{
		if(mCurrentTransactionInventory != null)
			endTransaction();
		
		mCurrentTransactionInventory = inventory;
		mCurrentTransactions = new ArrayList<ItemStack>();
		
		LogUtil.finer("Beginning Transaction");
	}
	public void doTransaction(ItemStack item, boolean take)
	{
		assert mCurrentTransactionInventory != null;
		
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
		assert mCurrentTransactionInventory != null;
		// Log what ever is left
		for(ItemStack transaction : mCurrentTransactions)
		{
			if(transaction.getAmount() != 0)
			{
				InventoryTransactionRecord record = null;
				
				if(transaction.getAmount() < 0)
				{
					transaction.setAmount(-transaction.getAmount());
					record = InventoryTransactionRecord.newTakeFromInventory(transaction, mCurrentTransactionInventory);
				}
				else
					record = InventoryTransactionRecord.newAddToInventory(transaction, mCurrentTransactionInventory);
				
				LogUtil.finer(record.toString());
				logRecord(record);
			}
		}
		
		LogUtil.finer("Ended Transaction");
		mCurrentTransactionInventory = null;
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
		logRecord(new GameModeRecord(event.getNewGameMode().ordinal()));
	}
	
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		logRecord(new InteractRecord(event.getAction(), event.getClickedBlock(), event.getItem(), null));
	}
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
	{
		logRecord(new InteractRecord(Action.RIGHT_CLICK_AIR,null, null, event.getRightClicked()));
	}
	
	public void onPlayerRespawn(PlayerRespawnEvent event)
	{
		
	}
	
	public void onPlayerShearEntity(PlayerShearEntityEvent event)
	{
		
	}
	
	public void onPlayerFish(PlayerFishEvent event)
	{
		
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
				LogUtil.finest("Target was shot by " + ((Projectile)damageEnt).getShooter().getType().toString());
				logRecord(new DamageRecord(((Projectile)damageEnt).getShooter(),damageAmount));
			}
			else
			{
				// We are being damaged
				LogUtil.finest("Target was damaged by entity");
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
		logRecord(new BlockChangeRecord(replaced.getBlock(), block, true));
	}
	public void onBlockBreak(Block block)
	{
		logRecord(new BlockChangeRecord(block, null, false));
	}
	public void onBucketFill(Block block, ItemStack resultant)
	{
		logRecord(new BlockChangeRecord(block, null, false));
	}
	public void onBucketEmpty(Block block, ItemStack resultant)
	{
		logRecord(new BlockChangeRecord(null, block, true));
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
}
