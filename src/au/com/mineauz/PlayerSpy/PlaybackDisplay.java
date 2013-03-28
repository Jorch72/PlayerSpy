package au.com.mineauz.PlayerSpy;

import java.util.ArrayList;
import java.util.LinkedList;

import net.minecraft.server.v1_5_R2.*;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_5_R2.CraftSound;
import org.bukkit.craftbukkit.v1_5_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import au.com.mineauz.PlayerSpy.Utilities.EntityShadowPlayer;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class PlaybackDisplay implements Listener
{
	/**
	 * Creates a new playback display. Be sure to register this with the bukkit event system
	 */
	public PlaybackDisplay()
	{
		mViewers = new ArrayList<Player>();
		
		mShadowPlayers = new ArrayList<EntityShadowPlayer>();
		mAddedPlayers = new LinkedList<EntityShadowPlayer>();
		mRemovedPlayers = new LinkedList<EntityShadowPlayer>();
		
		mShadowItems = new ArrayList<EntityItem>();
		mAddedItems = new LinkedList<EntityItem>();
		mRemovedItems = new LinkedList<EntityItem>();
		
		mShadowMobs = new ArrayList<EntityLiving>();
		mAddedMobs = new LinkedList<EntityLiving>();
		mRemovedMobs = new LinkedList<EntityLiving>();
		
		SpyPlugin.getInstance().getServer().getPluginManager().registerEvents(this, SpyPlugin.getInstance());
	}
	/**
	 * Adds a player to be a viewer of the playback
	 * @param player The player to add
	 */
	public void addViewer(Player player)
	{
		if(!mViewers.contains(player))
		{
			mViewers.add(player);
			String message = "[" + ChatColor.GREEN + "Playback" + ChatColor.WHITE + "] ";
			if(mShadowPlayers.size() > 0)
				message += "You have been added to the playback of " + mShadowPlayers.get(0).name;
			else
				message += "You have been added to a playback";
			
			player.sendMessage(message);
			
			// Resend all entities for this world
			for(EntityShadowPlayer ent : mShadowPlayers)
			{
				Packet20NamedEntitySpawn packet = new Packet20NamedEntitySpawn(ent);
				packet.b = "§e" + packet.b;
				sendPacketTo(player, packet, ent.world.getWorld());
				
				// Resend the equipment data
				doUpdateEquipment(ent);
			}
			
			for(EntityItem item : mShadowItems)
			{
				spawnEntityItem(item);
			}
			
			for(EntityLiving mob : mShadowMobs)
			{
				if(mob instanceof EntityHuman)
				{
					Packet20NamedEntitySpawn packet = new Packet20NamedEntitySpawn((EntityHuman)mob);
					packet.b = "§e" + packet.b;
					sendPacketTo(player, packet, mob.world.getWorld());
				}
				else
					sendPacketTo(player, new Packet24MobSpawn(mob), mob.world.getWorld());
			}
		}
	}
	/**
	 * Removes a player from being able to see the playback
	 * @param player
	 */
	public void removeViewer(Player player)
	{
		if(mViewers.contains(player))
		{
			mViewers.remove(player);
			String message = "[" + ChatColor.GREEN + "Playback" + ChatColor.WHITE + "] ";
			if(mShadowPlayers.size() > 0)
				message += "You have been removed from the playback of " + mShadowPlayers.get(0).name;
			else
				message += "You have been removed from a playback";
			
			player.sendMessage(message);
			
			
			cleanupFor(player);
		}
	}
	/**
	 * Removes all players from being able to see the playback
	 */
	public void removeAllViewers()
	{
		for(Player player : mViewers)
		{
			String message = "[" + ChatColor.GREEN + "Playback" + ChatColor.WHITE + "] ";
			if(mShadowPlayers.size() > 0)
				message += "You have been removed from the playback of " + mShadowPlayers.get(0).name;
			else
				message += "You have been removed from a playback";
			
			player.sendMessage(message);
			
			
			cleanupFor(player);
		}
		
		mViewers.clear();
	}
	
	/**
	 * Gets the number of players currently able to view this
	 * @return
	 */
	public int getViewerCount() 
	{
		return mViewers.size();
	}
	
	/**
	 * Sends a message to all the viewers
	 * @param message
	 */
	public void notifyViewers(String message)
	{
		message = "[" + ChatColor.GREEN + "Playback" + ChatColor.WHITE + "] " + message;
		for(Player viewer : mViewers)
		{
			viewer.sendMessage(message);
		}
	}
	
	/**
	 * Adds a fake player to the display. Make sure to use the methods to update it
	 */
	public void addShadowPlayer(EntityShadowPlayer player)
	{
		if(mShadowPlayers.contains(player))
			return;
		mShadowPlayers.add(player);
		mAddedPlayers.add(player);
	}
	/**
	 * Removes a fake player from display
	 */
	public void removeShadowPlayer(EntityShadowPlayer player)
	{
		if(mShadowPlayers.remove(player))
			mRemovedPlayers.add(player);
	}

	/**
	 * Adds a fake mob to the display. Make sure to use the methods to update it
	 */
	public void addShadowMob(EntityLiving mob)
	{
		if(mShadowMobs.contains(mob))
			return;
		mShadowMobs.add(mob);
		mAddedMobs.add(mob);
	}
	/**
	 * Removes a fake mob from display.
	 */
	public void removeShadowMob(EntityLiving mob)
	{
		if(mShadowMobs.remove(mob))
			mRemovedMobs.add(mob);
	}

	/**
	 * Adds a fake item to the display. Make sure to use the methods to update it
	 */
	public void addShadowItem(EntityItem item)
	{
		if(mShadowItems.contains(item))
			return;
		mShadowItems.add(item);
		mAddedItems.add(item);
	}
	/**
	 * Removes a fake item from display
	 */
	public void removeShadowItem(EntityItem item)
	{
		if(mShadowItems.remove(item))
			mRemovedItems.add(item);
	}
	
	/**
	 * Used to update what the viewer sees
	 */
	@EventHandler
	private void onViewerChangeWorld(PlayerChangedWorldEvent event)
	{
		if(mViewers.contains(event.getPlayer()))
		{
			// Resend all entities for this world
			for(EntityShadowPlayer ent : mShadowPlayers)
			{
				Packet20NamedEntitySpawn packet = new Packet20NamedEntitySpawn(ent);
				packet.b = "§e" + packet.b;
				sendPacketTo(event.getPlayer(), packet, ent.world.getWorld());
				
				// Resend the equipment data
				doUpdateEquipment(ent);
			}
			
			for(EntityItem item : mShadowItems)
			{
				spawnEntityItem(item);
			}
			
			for(EntityLiving mob : mShadowMobs)
			{
				if(mob instanceof EntityHuman)
				{
					Packet20NamedEntitySpawn packet = new Packet20NamedEntitySpawn((EntityHuman)mob);
					packet.b = "§e" + packet.b;
					sendPacketTo(event.getPlayer(), packet, mob.world.getWorld());
				}
				else
					sendPacketTo(event.getPlayer(), new Packet24MobSpawn(mob), mob.world.getWorld());
			}
		}
	}
	
	/**
	 * Resets everything back to how it actually is
	 * @param player The specific player to reset for. Set to null for all viewers
	 */
	private void cleanupFor(Player player)
	{
		int[] ids = new int[mShadowPlayers.size() + mShadowItems.size() + mShadowMobs.size() + mRemovedPlayers.size()];
		int i = 0;
		
		// Add all the ids
		for(EntityShadowPlayer ent : mRemovedPlayers)
			ids[i++] = ent.id;
		
		for(EntityShadowPlayer ent : mShadowPlayers)
			ids[i++] = ent.id;
		
		for(EntityLiving ent : mShadowMobs)
			ids[i++] = ent.id;
		
		for(EntityItem ent : mShadowItems)
			ids[i++] = ent.id;
		
		Packet29DestroyEntity packet = new Packet29DestroyEntity();
		packet.a = ids;
		
		// Send the packet
		if(player == null)
		{
			sendPacket(packet, null);
		}
		else
		{
			sendPacketTo(player, packet, null);
		}
	}
	
	/**
	 * Updates visuals
	 */
	public void update()
	{
		for(EntityShadowPlayer player : mRemovedPlayers)
		{
			sendPacket(new Packet29DestroyEntity(player.id), player.world.getWorld());
		}
		mRemovedPlayers.clear();
		
		for(EntityShadowPlayer player : mAddedPlayers)
		{
			Packet20NamedEntitySpawn packet = new Packet20NamedEntitySpawn(player);
			packet.b = "§e" + packet.b;
			sendPacket(packet, player.world.getWorld());
		}
		mAddedPlayers.clear();
		
		
		// Do updates
		for(EntityShadowPlayer player : mShadowPlayers)
		{
			updateEntity(player);
		}
		
		for(EntityItem item : mAddedItems)
		{
			spawnEntityItem(item);
		}
		mAddedItems.clear();
		
		for(EntityItem item : mRemovedItems)
		{
			sendPacket(new Packet29DestroyEntity(item.id), item.world.getWorld());
		}
		mRemovedItems.clear();
		
		
		for(EntityLiving mob : mAddedMobs)
		{
			if(mob instanceof EntityHuman)
			{
				Packet20NamedEntitySpawn packet = new Packet20NamedEntitySpawn((EntityHuman)mob);
				packet.b = "§e" + packet.b;
				sendPacket(packet, mob.world.getWorld());
			}
			else
				sendPacket(new Packet24MobSpawn(mob), mob.world.getWorld());
			
		}
		mAddedMobs.clear();
		
		for(EntityLiving mob : mRemovedMobs)
		{
			sendPacket(new Packet29DestroyEntity(mob.id), mob.world.getWorld());
		}
		mRemovedMobs.clear();
		
		// Do updates
		for(EntityLiving mob : mShadowMobs)
		{
			updateEntity(mob);
		}
	}
	
	private void updateEntity(Entity ent)
	{
		if(ent.positionChanged || (Math.abs(ent.lastYaw - ent.yaw) > 3 || Math.abs(ent.lastPitch - ent.pitch) > 3))
			doPositionUpdate(ent);
		
		if(ent.velocityChanged)
			doVelocityUpdate(ent);
		
		if(ent.getDataWatcher().a())
			doMetaDataUpdate(ent);
		
		if(ent instanceof EntityHuman)
		{
			if(((EntityHuman)ent).inventory.e)
			{
				doUpdateEquipment((EntityHuman)ent);
				((EntityHuman)ent).inventory.e = false;
			}
		}
	}
	public void doHeldItemUpdate(EntityHuman ent)
	{
		if(ent.inventory.items[ent.inventory.itemInHandIndex] == null || ent.inventory.items[ent.inventory.itemInHandIndex].id == 0)
		{
			Packet5EntityEquipment packet = new Packet5EntityEquipment(ent.id, 0, null);
			sendPacket(packet, ent.world.getWorld());
		}
		else
		{
			Packet5EntityEquipment packet = new Packet5EntityEquipment(ent.id, 0, ent.inventory.items[ent.inventory.itemInHandIndex]);
			sendPacket(packet, ent.world.getWorld());
		}
		
	}
	private void spawnEntityItem(EntityItem item)
	{
		sendPacket(new Packet23VehicleSpawn(item, 2, 1), item.world.getWorld());
		sendPacket(new Packet40EntityMetadata(item.id, item.getDataWatcher(), true), item.world.getWorld());
	}
	private void doUpdateEquipment(EntityHuman ent)
	{
		for(int i = 0; i < 4; i++)
		{
			if(ent.inventory.armor[i] != null && ent.inventory.armor[i].id != 0)
			{
				Packet5EntityEquipment packet = new Packet5EntityEquipment(ent.id, i+1, ent.inventory.armor[i]);
				sendPacket(packet, ent.world.getWorld());
			}
			else
			{
				Packet5EntityEquipment packet = new Packet5EntityEquipment(ent.id, i+1, null);
				sendPacket(packet, ent.world.getWorld());
			}
		}
		
		Packet5EntityEquipment packet = new Packet5EntityEquipment(ent.id, 0, (ent.inventory.items[ent.inventory.itemInHandIndex] == null || ent.inventory.items[ent.inventory.itemInHandIndex].id == 0 ? null : ent.inventory.items[ent.inventory.itemInHandIndex]));
		sendPacket(packet, ent.world.getWorld());
	}
	private void doMetaDataUpdate(Entity ent)
	{
		Packet40EntityMetadata packet = new Packet40EntityMetadata(ent.id, ent.getDataWatcher(), true);
		
		sendPacket(packet, ent.world.getWorld());
	}
	private void doPositionUpdate(Entity ent)
	{
//		double len = (ent.locX - ent.lastX) * (ent.locX - ent.lastX) +
//				(ent.locY - ent.lastY) * (ent.locY - ent.lastY) + 
//				(ent.locZ - ent.lastZ) * (ent.locZ - ent.lastZ);
//		
		Packet packet;
//		if(len >= 16)
			packet = new Packet34EntityTeleport(ent);
//		else
//			packet = new Packet33RelEntityMoveLook(ent.id, (byte)((ent.locX - ent.lastX) * 32D), (byte)((ent.locY - ent.lastY) * 32D), (byte)((ent.locZ - ent.lastZ) * 32D), (byte)(ent.yaw * 256D / 360D), (byte)(ent.pitch * 256D / 360D));
//		
		sendPacket(packet, ent.world.getWorld());
	}
	private void doVelocityUpdate(Entity ent)
	{
		Packet28EntityVelocity packet = new Packet28EntityVelocity(ent);
		sendPacket(packet, ent.world.getWorld());
	}
	public void doHeadLook(EntityLiving ent)
	{
		if(ent instanceof EntityShadowPlayer && !mShadowPlayers.contains(ent))
			addShadowPlayer((EntityShadowPlayer)ent);
		
		Packet32EntityLook look = new Packet32EntityLook(ent.id, (byte)(ent.aA * 256D / 360D), (byte)(ent.bc * 256D / 360D));
		Packet35EntityHeadRotation head = new Packet35EntityHeadRotation(ent.id, (byte)(ent.aA * 256D / 360D));
		
		sendPacket(look, ent.world.getWorld());
		sendPacket(head, ent.world.getWorld());
	}
	
	public void doArmAnimation(EntityHuman ent)
	{
		Packet18ArmAnimation packet = new Packet18ArmAnimation(ent, 1);
		sendPacket(packet, ent.world.getWorld());
	}
	public void doEntityDamage(EntityLiving ent)
	{
		Packet18ArmAnimation packet = new Packet18ArmAnimation(ent, 2);
		sendPacket(packet, ent.world.getWorld());
		playSound(Utility.getLocation(ent),Sound.HURT_FLESH, 1, 1);
	}
	public void pickupItem(EntityItem item, EntityHuman player)
	{
		if(mShadowItems.contains(item))
		{
			//LogUtil.finest("@" + player.id + " pickup " + item.id);
			Packet22Collect packet = new Packet22Collect(item.id, player.id);
			sendPacket(packet,player.world.getWorld());
			// Dont add it to the removed items list becuase collect also removes it
			mShadowItems.remove(item);
		}
	}
	
	public void doBlockChange(StoredBlock block)
	{
		Packet53BlockChange packet = new Packet53BlockChange();
		packet.a = block.getLocation().getBlockX();
		packet.b = block.getLocation().getBlockY();
		packet.c = block.getLocation().getBlockZ();
		
		packet.material = block.getTypeId();
		packet.data = block.getData();
		
		sendPacket(packet, block.getLocation().getWorld());
	}
	public void doChangeWorld(EntityHuman ent, World newWorld)
	{
		Packet29DestroyEntity packet1 = new Packet29DestroyEntity(ent.id);
		Packet20NamedEntitySpawn packet2 = new Packet20NamedEntitySpawn(ent);
		packet2.b = "§e" + packet2.b;
		
		sendPacket(packet1, ent.world.getWorld());
		ent.world = ((CraftWorld)newWorld).getHandle();
		sendPacket(packet2, ent.world.getWorld());
	}
	
	private void playSound(Location loc, Sound sound, float volume, float pitch)
	{
		Packet62NamedSoundEffect packet = new Packet62NamedSoundEffect(CraftSound.getSound(sound), loc.getX(), loc.getY(), loc.getZ(), volume, pitch);
		
		sendPacket(packet, loc.getWorld());
	}
	/**
	 * Sends a packet to all the viewers
	 * @param packet The packet to send
	 * @param world The world that they must be in to receive it, or null for any world
	 */
	private void sendPacket(Packet packet, World world)
	{
		for(Player viewer : mViewers)
		{
			if(world == null || viewer.getWorld() == world)
				((CraftPlayer)viewer).getHandle().playerConnection.sendPacket(packet);
		}
	}
	
	/**
	 * Sends a packet to a specific player
	 * @param player The player to send it to
	 * @param packet The packet to send
	 * @param world The world that they must be in to receive it, or null for any world
	 */
	private void sendPacketTo(Player player, Packet packet, World world)
	{
		if(world == null || player.getWorld() == world)
			((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
	}
	
	private ArrayList<Player> mViewers;
	
	private LinkedList<EntityShadowPlayer> mAddedPlayers;
	private LinkedList<EntityShadowPlayer> mRemovedPlayers;
	private ArrayList<EntityShadowPlayer> mShadowPlayers;
	
	private LinkedList<EntityItem> mAddedItems;
	private LinkedList<EntityItem> mRemovedItems;
	private ArrayList<EntityItem> mShadowItems;
	
	private LinkedList<EntityLiving> mAddedMobs;
	private LinkedList<EntityLiving> mRemovedMobs;
	private ArrayList<EntityLiving> mShadowMobs;
}
