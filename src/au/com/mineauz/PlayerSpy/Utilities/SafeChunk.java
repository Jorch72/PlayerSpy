package au.com.mineauz.PlayerSpy.Utilities;

import org.bukkit.Location;

public class SafeChunk
{
	public SafeChunk()
	{
		
	}
	public SafeChunk(Location location)
	{
		X = (location.getBlockX() / 16) * 16;
		Z = (location.getBlockZ() / 16) * 16;
		WorldHash = location.getWorld().getName().hashCode();
	}
	public int X;
	public int Z;
	public int WorldHash;
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof SafeChunk))
			return false;
		
		return ((SafeChunk)obj).X == X && ((SafeChunk)obj).Z == Z && ((SafeChunk)obj).WorldHash == WorldHash;
	}
	
	@Override
	public int hashCode()
	{
		int hash = 3;

        hash = 19 * hash + WorldHash;
        hash = 19 * hash + (int) (X ^ (X * 23) >>> 32);
        hash = 19 * hash + (int) (Z ^ (Z * 23) >>> 32);
        
        return hash;
	}
}
