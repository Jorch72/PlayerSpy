package au.com.mineauz.PlayerSpy.monitoring;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import au.com.mineauz.PlayerSpy.Records.ArmSwingRecord;
import au.com.mineauz.PlayerSpy.Records.HeldItemChangeRecord;
import au.com.mineauz.PlayerSpy.Records.MoveRecord;
import au.com.mineauz.PlayerSpy.Records.RespawnRecord;
import au.com.mineauz.PlayerSpy.Records.RightClickActionRecord;
import au.com.mineauz.PlayerSpy.Records.SleepRecord;
import au.com.mineauz.PlayerSpy.Records.SneakRecord;
import au.com.mineauz.PlayerSpy.Records.SprintRecord;
import au.com.mineauz.PlayerSpy.Records.TeleportRecord;

public class DeepMonitor extends ShallowMonitor
{
	public DeepMonitor(OfflinePlayer player) 
	{
		super(player);
	}
	public DeepMonitor(ShallowMonitor monitor)
	{
		super(monitor);
	}
	
	public void onMove(Location feet, Location head)
	{
		logRecord(new MoveRecord(feet, head));
	}
	
	public void onTeleport(Location newLocation, TeleportCause cause)
	{
		logRecord(new TeleportRecord(newLocation, cause));
	}
	
	public void onPlayerRespawn(PlayerRespawnEvent event)
	{
		logRecord(new RespawnRecord(event.getRespawnLocation()));
	}
	
	public void onPlayerAnimationEvent(PlayerAnimationEvent event)
	{
		if(event.getAnimationType() == PlayerAnimationType.ARM_SWING)
			logRecord(new ArmSwingRecord());
	}
	
	public void onPlayerSneakToggle(boolean sneak)
	{
		logRecord(new SneakRecord(sneak));
	}
	
	public void onPlayerSprintToggle(boolean sprint)
	{
		logRecord(new SprintRecord(sprint));
	}
	
	public void onHeldItemChange(int slot)
	{
		logRecord(new HeldItemChangeRecord(slot));
	}
	
	public void onSleep(Location location, boolean start)
	{
		logRecord(new SleepRecord(start, location));
	}
	
	public void onPlayerFish(PlayerFishEvent event)
	{
		if(event.getState() == State.FISHING)
		{
			// Cast out
			logRecord(new RightClickActionRecord(RightClickActionRecord.Action.FishCast, null, null));
		}
		else if(event.getState() == State.CAUGHT_ENTITY)
		{
			logRecord(new RightClickActionRecord(RightClickActionRecord.Action.FishPullback, null, event.getCaught()));
		}
		else if(event.getState() == State.CAUGHT_FISH)
		{
			logRecord(new RightClickActionRecord(RightClickActionRecord.Action.FishPullback, ((Item)event.getCaught()).getItemStack(), null));
		}
		else
		{
			logRecord(new RightClickActionRecord(RightClickActionRecord.Action.FishPullback, null, null));
		}
	}
}
