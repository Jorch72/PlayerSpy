package au.com.mineauz.PlayerSpy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;

import net.minecraft.server.v1_4_6.EntityItem;
import net.minecraft.server.v1_4_6.EntityLiving;
import net.minecraft.server.v1_4_6.MathHelper;
import net.minecraft.server.v1_4_6.PlayerInventory;
import net.minecraft.server.v1_4_6.World;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_4_6.CraftWorld;
import org.bukkit.craftbukkit.v1_4_6.inventory.CraftInventoryPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import au.com.mineauz.PlayerSpy.PlaybackControl.PlaybackState;
import au.com.mineauz.PlayerSpy.Records.*;
import au.com.mineauz.PlayerSpy.Utilities.EntityShadowPlayer;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class PlaybackContext
{
	public PlaybackContext(LogFile logFile)
	{
		mDisplay = new PlaybackDisplay();
		
		mPrimaryController = new PlaybackControl(new RecordBuffer(logFile), new Callable<Void>() {

			@Override
			public Void call() throws Exception 
			{
				// Seek callback
				mDisplay.notifyViewers("at " + ChatColor.GREEN + Utility.formatTime(mPrimaryController.getPlaybackDate(), "dd/MM/yy HH:mm:ss"));
				
				if(mPrimaryController.getBuffer().size() > 0 && mPrimaryController.getBufferIndex() < mPrimaryController.getBuffer().size())
				{
					if(!mIndexShadowPlayerMap.containsKey(0))
					{
						// Add the target to the display
						Location loc = mPrimaryController.getBuffer().getCurrentLocation(mPrimaryController.getBufferIndex());
						if(loc == null)
							loc = mPrimaryController.getBuffer().getFirstLocation();
						
						EntityShadowPlayer target = createShadowPlayer(mPrimaryController.getPlayer(), loc);
						mDisplay.addShadowPlayer(target);
						mIndexShadowPlayerMap.put(0,target);
					}
					else
					{
						Location loc = mPrimaryController.getBuffer().getCurrentLocation(mPrimaryController.getBufferIndex());
						if(loc == null)
							loc = mPrimaryController.getBuffer().getFirstLocation();
						
						EntityShadowPlayer target = mIndexShadowPlayerMap.get(0);
						Utility.setEntityPosition(target, loc);
					}
				}
				
				// Remove existing items
				for(Entry<Integer, EntityItem> ent : mIndexShadowItemMap.entrySet())
				{
					mDisplay.removeShadowItem(ent.getValue());
				}
				mIndexShadowItemMap.clear();
				
				// Remove existing entities
				for(Entry<Integer, EntityLiving> ent : mIndexShadowMobMap.entrySet())
				{
					mDisplay.removeShadowMob(ent.getValue());
				}
				mIndexShadowMobMap.clear();
				mStoredIdEntityIdMap.clear();
				
				// Restore blocks
				for(Entry<Integer, StoredBlock> ent : mIndexBlockMap.entrySet())
				{
					mDisplay.doBlockChange(ent.getValue());
				}
				mIndexBlockMap.clear();
				mPreprocessIndex = mPostProcessIndex = mPrimaryController.getAbsoluteIndex();
				
				for(Entry<Integer, EntityShadowPlayer> ent : mIndexShadowPlayerMap.entrySet())
				{
					InventoryRecord invRecord = mPrimaryController.getBuffer().getCurrentInventory(mPrimaryController.getBufferIndex());
					if(invRecord == null)
						invRecord = mPrimaryController.getBuffer().getFirstInventory();
					
					if(invRecord != null)
					{
						for(int i = 0; i < invRecord.getItems().length; i++)
						{
							if(invRecord.getItems()[i] != null)
								ent.getValue().inventory.items[i] = Utility.convertToNative(invRecord.getItems()[i]);
						}
						for(int i = 0; i < invRecord.getArmour().length; i++)
						{
							if(invRecord.getArmour()[i] != null)
								ent.getValue().inventory.armor[i] = Utility.convertToNative(invRecord.getArmour()[i]);
						}
						ent.getValue().inventory.itemInHandIndex = invRecord.getHeldSlot();
					}
					else
					{
						Arrays.fill(ent.getValue().inventory.items, null);
						Arrays.fill(ent.getValue().inventory.armor, null);
					}
					
					ent.getValue().inventory.update();
					break;
					
				}

				return null;
			}
			
		}, new Callable<Void>() {

			@Override
			public Void call() throws Exception 
			{
				// Finish Callback
				mDisplay.notifyViewers("Playback has run out of data to display. Please seek to another time or exit.");
				return null;
			}
			
		}, new Callable<Void>() {
			
			@Override
			public Void call() throws Exception 
			{
				// TODO: Display more relavent detail
				mDisplay.notifyViewers("Unable to find any results");
				return null;
			}
		}, new Callable<Void>() {
			
			@Override
			public Void call() throws Exception
			{
				// Deep mode switch
				mDisplay.notifyViewers("Playback will now be normal.");
				return null;
			}
		}, new Callable<Void>() {
			
			@Override
			public Void call() throws Exception
			{
				// Shallow mode switch
				mDisplay.notifyViewers("Playback will now be limited as recording was switched to shallow mode.");
				return null;
			}
		});
		
		mRandom = new Random();
		mIndexShadowItemMap = new HashMap<Integer, EntityItem>();
		mIndexShadowMobMap = new HashMap<Integer, EntityLiving>();
		mIndexShadowPlayerMap = new HashMap<Integer, EntityShadowPlayer>();
		mIndexBlockMap = new HashMap<Integer, StoredBlock>();
		mStoredIdEntityIdMap = new HashMap<Integer, EntityLiving>();
		
	}
	
	/**
	 * Plays this context
	 * @return True if playback was started. False if you need to specify a time with seek()
	 */
	public boolean play()
	{
		if(mPrimaryController.play())
		{
			mDisplay.notifyViewers("Playback started");
			return true;
		}
		return false;
	}
	/**
	 * Pauses this context
	 */
	public void pause()
	{
		mPrimaryController.pause();
		mDisplay.notifyViewers("Playback paused");
	}
	
	/**
	 * Closes this context. After this you may not call any other methods in this context.
	 */
	public void close()
	{
		mPrimaryController.close();
		
		mDisplay.removeAllViewers();
		
		// Remove existing items
		for(Entry<Integer, EntityItem> ent : mIndexShadowItemMap.entrySet())
		{
			mDisplay.removeShadowItem(ent.getValue());
		}
		mIndexShadowItemMap.clear();
		
		// Remove existing entities
		for(Entry<Integer, EntityLiving> ent : mIndexShadowMobMap.entrySet())
		{
			mDisplay.removeShadowMob(ent.getValue());
		}
		mIndexShadowMobMap.clear();
		mStoredIdEntityIdMap.clear();
		
		// Restore blocks
		for(Entry<Integer, StoredBlock> ent : mIndexBlockMap.entrySet())
		{
			mDisplay.doBlockChange(ent.getValue());
		}
		mIndexBlockMap.clear();
		
		// Remove targets
		for(Entry<Integer, EntityShadowPlayer> ent : mIndexShadowPlayerMap.entrySet())
		{
			mDisplay.removeShadowPlayer(ent.getValue());
		}
		mIndexShadowPlayerMap.clear();
	}
	
	/**
	 * Seeks the context to a specified date
	 * @param date The date to seek to
	 * @return True if the seek request was successful
	 */
	public boolean seek(long date)
	{
		return mPrimaryController.seek(date);
	}
	
	/**
	 * Seeks to a block of that material being mined or placed
	 * @param block The type of block to look for
	 * @param mined True if the block was mined, false if it was placed
	 * @param date The date to search from. Set to 0 to ignore
	 * @param before True if to search backwards starting from the date. False for forwards
	 * @return True if the seek request was successful
	 */
	public boolean seekToBlock(Material block, boolean mined, long date, boolean before)
	{
		return mPrimaryController.seekToBlock(block, mined, date, before);
	}
	
	/**
	 * Seeks to an item of that material being mined or placed
	 * @param item The type of item to look for
	 * @param gained True if the item was picked up or gained, false if it was dropped or placed somewhere
	 * @param date The date to search from. Set to 0 to ignore
	 * @param before True if to search backwards starting from the date. False for forwards
	 * @return True if the seek request was successful
	 */
	public boolean seekToItem(Material item, boolean gained, long date, boolean before)
	{
		return mPrimaryController.seekToItem(item, gained, date, before);
	}
	/**
	 * Seeks to an event of that type
	 * @param type The type of event to search for
	 * @param date The date to search from. Set to 0 to ignore
	 * @param before True if to search backwards starting from the date. False for forwards
	 * @return True if the seek request was successful
	 */
	public boolean seekToEvent(RecordType type, long date, boolean before)
	{
		return mPrimaryController.seekToEvent(type, date, before);
	}
	
	/**
	 * Seeks to an attack or damage record
	 * @param entType The type of entity to look for
	 * @param attack True if looking for attack record, false if looking for damage records
	 * @param playerName If entType is player then you can set this to a player name. This can be null
	 * @param date The date to search from. Set to 0 to ignore
	 * @param before True if to search backwards starting from the date. False for forwards
	 * @return True if the seek request was successful
	 */
	public boolean seekToDamage(EntityType entType, boolean attack, String playerName, long date, boolean before)
	{
		return mPrimaryController.seekToDamage(entType, attack, playerName, date, before);
	}
	/**
	 * Skips the context by time.
	 * @param time The time to skip. Can be negative and can be 0 (in which case it will go to the next available record)
	 * @return true if the skip request was successful
	 */
	public boolean skip(long time)
	{
		return mPrimaryController.skip(time);
	}
	
	/**
	 * Adds a viewer to this context
	 * @param player The player to add
	 */
	public void addViewer(Player player)
	{
		mDisplay.addViewer(player);
	}
	/**
	 * Removes a viewer from this context
	 * @param player The player to remove
	 */
	public void removeViewer(Player player)
	{
		mDisplay.removeViewer(player);
	}
	
	/**
	 * Gets the number of viewers currently able to see this context
	 */
	public int getViewerCount()
	{
		return mDisplay.getViewerCount();
	}

	/**
	 * Gets the current date of the playback
	 */
	public long getPlaybackDate()
	{
		return mPrimaryController.getPlaybackDate();
	}

	/**
	 * Gets the earliest date available
	 */
	public long getStartDate()
	{
		return mPrimaryController.getStartDate();
	}

	/**
	 * Gets the latest date available
	 * @return
	 */
	public long getEndDate()
	{
		return mPrimaryController.getEndDate();
	}
	/**
	 * Adds another log file to playback. 
	 * WARNING: adding more logfiles increases memory usage substantially. 
	 */
	public void addTarget(LogFile logFile)
	{
		// TODO: Add target
	}
	/**
	 * Removes a log file added with addTarget() from this context.
	 */
	public void removeTarget(LogFile logFile)
	{
		// TODO: Remove target
	}

	/**
	 * Gets the number of targets in this context
	 */
	public int getTargetCount()
	{
		// TODO: target count
		return 1;
	}
	
	/**
	 * Gets the name of a target
	 * @param index The zero based index of the target
	 */
	public String getTargetName(int index)
	{
		if(index == 0)
		{
			return mPrimaryController.getPlayer();
		}
		// TODO: target name
		return null;
	}
	/**
	 * Gets the current location of the target
	 * @param index The zero based index of the target
	 */
	public Location getTargetLocation(int index)
	{
		if(mIndexShadowPlayerMap.containsKey(index))
		{
			EntityShadowPlayer player = mIndexShadowPlayerMap.get(index);
			return new Location(player.world.getWorld(), player.locX, player.locY, player.locZ, player.yaw, player.pitch);
		}
		
		LogUtil.fine("getTargetLocation() index was invalid");
		
		return null;
	}
	
	/**
	 * Gets the current inventory of the target
	 * @param index The zero based index of the target
	 */
	public Inventory getTargetInventory(int index)
	{
		if(mIndexShadowPlayerMap.containsKey(index))
		{
			EntityShadowPlayer player = mIndexShadowPlayerMap.get(index);
			
			return new CraftInventoryPlayer(player.inventory);
		}
		
		return null;
	}
	/**
	 * Updates the playback, loads buffers, updates display etc.
	 * This MUST be called every tick 
	 */
	public void update()
	{
		// Update the primary controller
		int searchIdStart = mPrimaryController.getAbsoluteIndex()+1;
		PlaybackState state = mPrimaryController.getState();
		mPrimaryController.update();
		
		// It changed to buffering
		if(mPrimaryController.getState() == PlaybackState.Buffering && state != mPrimaryController.getState())
		{
			mDisplay.notifyViewers("Buffering, Please Wait");
		}
		
		// Do our update
		if(mPrimaryController.getState() == PlaybackState.Playing)
		{
			EntityShadowPlayer player = mIndexShadowPlayerMap.get(0);
			if(player != null)
			{
				for(int i = searchIdStart; i <= mPrimaryController.getAbsoluteIndex(); i++)
				{
					int bufferIndex = i - mPrimaryController.getRelativeOffset();
					process(mPrimaryController.getBuffer().get(bufferIndex), player, i);
				}
			}
		}
		
		// Do preprocessing for upcomming records
		EntityShadowPlayer player = mIndexShadowPlayerMap.get(0);
		if(player != null)
		{
			for(int i = mPreprocessIndex - mPrimaryController.getRelativeOffset(); i < mPrimaryController.getBuffer().size(); i++)
			{
				// Seeks backwards might make this happen
				if(i < 0)
					i = 0;
				
				if(mPrimaryController.getBuffer().get(i).getTimestamp() > mPrimaryController.getPlaybackDate() + sLookaheadTime)
					break;
				
				mPreprocessIndex = i + mPrimaryController.getRelativeOffset();
				
				preProcess(mPrimaryController.getBuffer().get(i), player, mPreprocessIndex);
			}
		}
		
		// Do post processing for processed records
		for(int i = mPostProcessIndex - mPrimaryController.getRelativeOffset(); i < mPrimaryController.getBufferIndex(); i++)
		{
			if( i >= 0 )
			{
				Record r = mPrimaryController.getBuffer().get(i);
				if(r.getTimestamp() > mPrimaryController.getPlaybackDate() - sCleanupTime)
					break;
			}
			
			mPostProcessIndex = i + mPrimaryController.getRelativeOffset();
			postProcess(mPostProcessIndex);
		}
		
		// Update display
		mDisplay.update();
	}
	
	private void process(Record record, EntityShadowPlayer forPlayer,int id)
	{
		switch(record.getType())
		{
		case ArmSwing:
			mDisplay.doArmAnimation(forPlayer);
			break;
		case Attack:
		{
			// Move the mob
			AttackRecord arecord = (AttackRecord)record;
			if(mStoredIdEntityIdMap.containsKey(arecord.getDamagee().getEntityId()))
			{
				if(arecord.getDamage() == -1)
				{
					//LogUtil.finest("Kill");
					// Remove it
					mDisplay.removeShadowMob(mStoredIdEntityIdMap.get(arecord.getDamagee().getEntityId()));
					mIndexShadowMobMap.remove(id);
					mStoredIdEntityIdMap.remove(arecord.getDamagee().getEntityId());
				}
				else
				{
					// Update its position
					Utility.setEntityPosition(mStoredIdEntityIdMap.get(arecord.getDamagee().getEntityId()), arecord.getDamagee().getLocation());
					mDisplay.doEntityDamage(mStoredIdEntityIdMap.get(arecord.getDamagee().getEntityId()));
				}
				
				
			}
			break;
		}
		case BlockChange:
		{
			mDisplay.doBlockChange(((BlockChangeRecord)record).getFinalBlock());
			break;
		}
		case ChatCommand:
			mDisplay.notifyViewers(forPlayer.name + ": " + ((ChatCommandRecord)record).getMessage());
			break;
		case Damage:
		{
			// Move the mob
			DamageRecord drecord = (DamageRecord)record;
			if(drecord.getDamager() != null)
			{
				if(mStoredIdEntityIdMap.containsKey(drecord.getDamager().getEntityId()))
				{
					Utility.setEntityPosition(mStoredIdEntityIdMap.get(drecord.getDamager().getEntityId()), drecord.getDamager().getLocation());
				}
			}
			mDisplay.doEntityDamage(forPlayer);
			break;
		}
		case Death:
			mDisplay.notifyViewers(ChatColor.YELLOW + ((DeathRecord)record).getReason());
			mDisplay.removeShadowPlayer(forPlayer);
			break;
		case EndOfSession:
			break;
		case FullInventory:
		{
			InventoryRecord invRecord = (InventoryRecord)record;
			for(int i = 0; i < invRecord.getItems().length; i++)
			{
				if(invRecord.getItems()[i] != null)
					forPlayer.inventory.items[i] = Utility.convertToNative(invRecord.getItems()[i]);
			}
			for(int i = 0; i < invRecord.getArmour().length; i++)
			{
				if(invRecord.getArmour()[i] != null)
					forPlayer.inventory.armor[i] = Utility.convertToNative(invRecord.getArmour()[i]);
			}
			
			forPlayer.inventory.itemInHandIndex = invRecord.getHeldSlot();
			forPlayer.inventory.update();
			break;
		}
		case GameMode:
		{
			GameMode gm = GameMode.getByValue(((GameModeRecord)record).getGameMode());
			switch(gm)
			{
			case CREATIVE:
				mDisplay.notifyViewers(forPlayer.name + "'s gamemode was changed to Creative");
				break;
			case SURVIVAL:
				mDisplay.notifyViewers(forPlayer.name + "'s gamemode was changed to Survival");
				break;
			case ADVENTURE:
				mDisplay.notifyViewers(forPlayer.name + "'s gamemode was changed to Adventure");
				break;
			}
			break;
		}
		case HeldItemChange:
			forPlayer.inventory.itemInHandIndex = ((HeldItemChangeRecord)record).getSlot();
			mDisplay.doHeldItemUpdate(forPlayer);
			break;
		case Interact:
			break;
		case ItemPickup:
			if(mIndexShadowItemMap.containsKey(id))
			{
				mDisplay.pickupItem(mIndexShadowItemMap.get(id), forPlayer);
				mIndexShadowItemMap.remove(id);
			}
			else
				LogUtil.fine("WARNING: Dropped item was not there to pickup");
			break;
		case Login:
			Utility.setEntityPosition(forPlayer, ((LoginRecord)record).getLocation());
			mDisplay.addShadowPlayer(forPlayer);
			
			mDisplay.notifyViewers(ChatColor.YELLOW + forPlayer.name + " joined the game");
			break;
		case Logoff:
			switch(((LogoffRecord)record).getLogoffType())
			{
			case Quit:
				mDisplay.notifyViewers(ChatColor.YELLOW + forPlayer.name + " left the game");
				break;
			case Kick:
				mDisplay.notifyViewers(ChatColor.RED + forPlayer.name + " was kicked for: " + ((LogoffRecord)record).getReason());
				break;
			case Ban:
				mDisplay.notifyViewers(ChatColor.RED + forPlayer.name + " was banned for: " + ((LogoffRecord)record).getReason());
				break;
			}
			mDisplay.removeShadowPlayer(forPlayer);
			break;
		case Move:
			Utility.setEntityPosition(forPlayer, ((MoveRecord)record).getLocation());
			Utility.setEntityHeadLook(forPlayer, ((MoveRecord)record).getHeadLocation().getYaw(), ((MoveRecord)record).getHeadLocation().getPitch());
			mDisplay.doHeadLook(forPlayer);
			break;
		case Respawn:
			Utility.setEntityPosition(forPlayer, ((RespawnRecord)record).getLocation());
			mDisplay.addShadowPlayer(forPlayer);
			break;
		case Sneak:
			forPlayer.setSneaking(((SneakRecord)record).isEnabled());
			break;
		case Sprint:
			forPlayer.setSprinting(((SprintRecord)record).isEnabled());
			break;
		case Teleport:
			Utility.setEntityPosition(forPlayer, ((TeleportRecord)record).getLocation());
			break;
		case UpdateInventory:
			for(InventorySlot slot : ((UpdateInventoryRecord)record).Slots)
			{
				if(slot.Slot >= forPlayer.inventory.items.length)
					forPlayer.inventory.armor[slot.Slot - forPlayer.inventory.items.length] = Utility.convertToNative(slot.Item);
				else
					forPlayer.inventory.items[slot.Slot] = Utility.convertToNative(slot.Item);
			}
			forPlayer.inventory.update();
			break;
		case WorldChange:
		{
			if(((WorldChangeRecord)record).getWorld() != forPlayer.world.getWorld())
			{
				mDisplay.doChangeWorld(forPlayer, ((WorldChangeRecord)record).getWorld());
			}
			break;
		}
		case DropItem:
		{
			EntityItem droppedItem = Utility.makeEntityItem(Utility.getLocation(forPlayer).add(0,forPlayer.height - 0.3D,0), ((DropItemRecord)record).getItem());
			// Angle it so that it fires in the direction aimed
			float f = 0.3F;
			droppedItem.motX = (-MathHelper.sin(forPlayer.yaw / 180.0F * 3.141593F) * MathHelper.cos(forPlayer.pitch / 180.0F * 3.141593F) * f);
			droppedItem.motZ = (MathHelper.cos(forPlayer.yaw / 180.0F * 3.141593F) * MathHelper.cos(forPlayer.pitch / 180.0F * 3.141593F) * f);
			droppedItem.motY = (-MathHelper.sin(forPlayer.pitch / 180.0F * 3.141593F) * f + 0.1F);
			f = 0.02F;
			float f1 = mRandom.nextFloat() * 3.141593F * 2.0F;
			f *= mRandom.nextFloat();
			droppedItem.motX += Math.cos(f1) * f;
			droppedItem.motY += (mRandom.nextFloat() - mRandom.nextFloat()) * 0.1F;
			droppedItem.motZ += Math.sin(f1) * f;
			
			mDisplay.addShadowItem(droppedItem);
			mIndexShadowItemMap.put(id, droppedItem);
			break;
		}
		case PaintingChange:
			break;
		case RClickAction:
			break;
		case Sleep:
			forPlayer.sleeping = ((SleepRecord)record).isSleeping();
			break;
		case VehicleMount:
			break;
		case ItemTransaction:
			break;
		default:
			break;
		}
	}
	
	/**
	 * Pre processes records (eg. changing blocks, spawning items/mobs)
	 */
	private void preProcess(Record record, EntityShadowPlayer player, int id)
	{
		switch(record.getType())
		{
		case ItemPickup:
		{
			//LogUtil.finest("Preparing for item pickup @" + id);
			ItemPickupRecord irecord = (ItemPickupRecord)record;
			EntityItem item = Utility.makeEntityItem(irecord.getLocation(), irecord.getItemStack());
			//item.id = PlaybackDisplay.getNextEntityId();
			
			mIndexShadowItemMap.put(id, item);
			mDisplay.addShadowItem(item);
			
			break;
		}
		case BlockChange:
		{
			BlockChangeRecord brecord = (BlockChangeRecord)record;
			
			StoredBlock existing = new StoredBlock(brecord.getBlock().getLocation().getBlock());
			StoredBlock block = brecord.getInitialBlock();
			
			mIndexBlockMap.put(id, existing);
			mDisplay.doBlockChange(block);
			break;
		}
		case Attack:
		{
			AttackRecord arecord = (AttackRecord)record;
			if(arecord.getDamagee().getEntityType().isAlive())
			{
				if(!mStoredIdEntityIdMap.containsKey(arecord.getDamagee().getEntityId()))
				{
					EntityLiving mob = (EntityLiving)arecord.getDamagee().createEntity();
					mStoredIdEntityIdMap.put(arecord.getDamagee().getEntityId(), mob);
					mIndexShadowMobMap.put(id, mob);
					mDisplay.addShadowMob(mob);
				}
				else
				{
					EntityLiving mob = mStoredIdEntityIdMap.get(arecord.getDamagee().getEntityId());
					for(Entry<Integer, EntityLiving> ent : mIndexShadowMobMap.entrySet())
					{
						if(ent.getValue() == mob)
						{
							mIndexShadowMobMap.remove(ent.getKey());
							mIndexShadowMobMap.put(id, mob);
							break;
						}
					}
				}
			}
			break;
		}
		case Damage:
		{
			DamageRecord drecord = (DamageRecord)record;
			if(drecord.getDamager() != null)
			{
				if(drecord.getDamager().getEntityType().isAlive())
				{
					if(!mStoredIdEntityIdMap.containsKey(drecord.getDamager().getEntityId()))
					{
						EntityLiving mob = (EntityLiving)drecord.getDamager().createEntity();
						mStoredIdEntityIdMap.put(drecord.getDamager().getEntityId(), mob);
						mIndexShadowMobMap.put(id, mob);
						mDisplay.addShadowMob(mob);
					}
					else
					{
						EntityLiving mob = mStoredIdEntityIdMap.get(drecord.getDamager().getEntityId());
						for(Entry<Integer, EntityLiving> ent : mIndexShadowMobMap.entrySet())
						{
							if(ent.getValue() == mob)
							{
								mIndexShadowMobMap.remove(ent.getKey());
								mIndexShadowMobMap.put(id, mob);
								break;
							}
						}
					}
				}
			}
			break;
		}
		default:
			break;
		}
	}
	
	/**
	 * Post process records (reseting things back to how they should be etc.)
	 * @param id The id of the record to post process
	 */
	private void postProcess(int id)
	{
		// Reset block changes
		if(mIndexBlockMap.containsKey(id))
		{
			mDisplay.doBlockChange(mIndexBlockMap.get(id));
			mIndexBlockMap.remove(id);
		}
		
		// Delete dropped items
		if(mIndexShadowItemMap.containsKey(id))
		{
			mDisplay.removeShadowItem(mIndexShadowItemMap.get(id));
			mIndexShadowItemMap.remove(id);
		}
		
		// delete mobs
		if(mIndexShadowMobMap.containsKey(id))
		{
			EntityLiving mob = mIndexShadowMobMap.get(id);
			for(Entry<Integer, EntityLiving> ent : mStoredIdEntityIdMap.entrySet())
			{
				if(ent.getValue() == mob)
				{
					mStoredIdEntityIdMap.remove(ent.getKey());
					break;
				}
			}
			
			mDisplay.removeShadowMob(mob);
			mIndexShadowMobMap.remove(id);
		}
	}
	/**
	 * Creates a shadow player
	 * @param name The name of the shadow player
	 * @param location The position and facing to place the player
	 * @return The newly formed instance
	 */
	private EntityShadowPlayer createShadowPlayer(String name, Location location)
	{
		World world = ((CraftWorld)location.getWorld()).getHandle();
		
		EntityShadowPlayer player = new EntityShadowPlayer(world, name);
		player.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
		player.inventory = new PlayerInventory(player);
		
		return player;
	}
	
	private int mPreprocessIndex;
	private int mPostProcessIndex;
	
	private HashMap<Integer, EntityShadowPlayer> mIndexShadowPlayerMap;
	private HashMap<Integer, EntityLiving> mIndexShadowMobMap;
	private HashMap<Integer, EntityLiving> mStoredIdEntityIdMap;
	private HashMap<Integer, EntityItem> mIndexShadowItemMap;
	private HashMap<Integer, StoredBlock> mIndexBlockMap;
	
	// The main controller that also controlls all the rest of them
	private PlaybackControl mPrimaryController;
	// Secondary controllers for added targets. These are not controlled directly but through the primary controller
	//private HashMap<String, PlaybackControl> mSecondaryControllers;
	// The display device
	private PlaybackDisplay mDisplay;
	
	private Random mRandom;
	/**
	 * The amount of time in milliseconds to search ahead in the buffer for things that need to be setup (eg. block changes, item spawns/pickups, Taking Damage, Attacking)
	 */
	public static long sLookaheadTime = 2000L;
	/**
	 * The amount of time after the fact after which any changes made to the world are reverted (eg. resetting changed blocks, removing item/mob spawns)
	 */
	public static long sCleanupTime = 20000L;
}
