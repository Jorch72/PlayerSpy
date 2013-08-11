package au.com.mineauz.PlayerSpy.Utilities;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.World;

public class CubicChunk
{
	public int chunkX;
	public int chunkZ;
	public byte chunkY;
	public int worldHash;
	
	public CubicChunk()
	{
		chunkX = chunkZ = 0;
		chunkY = (byte)0;
		worldHash = 0;
	}

	public CubicChunk(Location location)
	{
		chunkX = location.getBlockX() >> 4;
		chunkZ = location.getBlockZ() >> 4;
		chunkY = (byte)(location.getBlockY() >> 4);
		worldHash = location.getWorld().getUID().hashCode();
	}
	public CubicChunk(int x, int y, int z, World world)
	{
		chunkX = x;
		chunkZ = z;
		chunkY = (byte)y;
		worldHash = world.getUID().hashCode();
	}
	
	@Override
	public int hashCode()
	{
		return (chunkX * 3) | (chunkZ * 9 << 8) | (chunkY * 17 << 17) | worldHash;
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof CubicChunk))
			return false;
		
		CubicChunk chunk = (CubicChunk)obj;
		
		return chunk.chunkX == chunkX && chunk.chunkY == chunkY && chunk.chunkZ == chunkZ && chunk.worldHash == worldHash;
	}
	
	@Override
	public String toString()
	{
		return "Chunk: " + chunkX + ", " + chunkY + ", " + chunkZ;
	}
	
	public void write(DataOutput output) throws IOException
	{
		output.writeInt(chunkX);
		output.writeInt(chunkZ);
		output.writeByte(chunkY);
		
		output.writeInt(worldHash);
	}
	
	public void read(DataInput input) throws IOException
	{
		chunkX = input.readInt();
		chunkZ = input.readInt();
		
		chunkY = input.readByte();
		
		worldHash = input.readInt();
	}
	
	public static int getSize()
	{
		return 13; 
	}
	
	public boolean isPresent(Location location)
	{
		int x = location.getBlockX() >> 4;
		int y = location.getBlockY() >> 4;
		int z = location.getBlockZ() >> 4;
		int hash = location.getWorld().getUID().hashCode();
		
		return chunkX == x && chunkY == y && chunkZ == z && worldHash == hash;
	}
	
	public boolean isPresent(Location location, double range)
	{
		double totalDist = 0;
		
		if(location.getX() < chunkX * 16)
		{
			double s = location.getX() - chunkX * 16;
			totalDist += s*s;
		}
		else if(location.getX() > chunkX * 16 + 15)
		{
			double s = location.getX() - chunkX * 16 + 15;
			totalDist += s*s;
		}
		
		if(location.getY() < chunkY * 16)
		{
			double s = location.getY() - chunkY * 16;
			totalDist += s*s;
		}
		else if(location.getY() > chunkY * 16 + 15)
		{
			double s = location.getY() - chunkY * 16 + 15;
			totalDist += s*s;
		}
		
		if(location.getZ() < chunkZ * 16)
		{
			double s = location.getZ() - chunkZ * 16;
			totalDist += s*s;
		}
		else if(location.getZ() > chunkZ * 16 + 15)
		{
			double s = location.getZ() - chunkZ * 16 + 15;
			totalDist += s*s;
		}
		
		return totalDist <= range * range;
	}
}
