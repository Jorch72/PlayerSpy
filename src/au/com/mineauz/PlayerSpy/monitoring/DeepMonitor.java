package au.com.mineauz.PlayerSpy.monitoring;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import au.com.mineauz.PlayerSpy.Records.MoveRecord;

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
}
