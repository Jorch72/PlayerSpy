package au.com.mineauz.PlayerSpy.Records;

import org.bukkit.Location;

public interface ILocationAware 
{
	public Location getLocation();
	public boolean isFullLocation();
}
