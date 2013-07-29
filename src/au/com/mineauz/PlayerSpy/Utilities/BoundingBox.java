package au.com.mineauz.PlayerSpy.Utilities;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

public class BoundingBox
{
	public Vector min;
	public Vector max;
	
	public BoundingBox()
	{
		min = new BlockVector(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		max = new BlockVector(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
	}
	
	public BoundingBox(BlockVector a, BlockVector b)
	{
		min = a;
		max = b;
	}

	public void addPoint(Location loc)
	{
		BlockVector vec = new BlockVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		min = Vector.getMinimum(min, vec);
		max = Vector.getMaximum(max, vec);
	}
	
	public boolean intersects(Location loc, double radius)
	{
		double totalDist = 0;
		
		if(loc.getX() < min.getX())
		{
			double s = loc.getX() - min.getX();
			totalDist += s*s;
		}
		else if(loc.getX() > max.getX())
		{
			double s = loc.getX() - max.getX();
			totalDist += s*s;
		}
		
		if(loc.getY() < min.getY())
		{
			double s = loc.getY() - min.getY();
			totalDist += s*s;
		}
		else if(loc.getY() > max.getY())
		{
			double s = loc.getY() - max.getY();
			totalDist += s*s;
		}
		
		if(loc.getZ() < min.getZ())
		{
			double s = loc.getZ() - min.getZ();
			totalDist += s*s;
		}
		else if(loc.getZ() > max.getZ())
		{
			double s = loc.getZ() - max.getZ();
			totalDist += s*s;
		}
		
		return totalDist <= radius * radius;
	}
	
	public boolean isContained(Location loc)
	{
		BlockVector vec = new BlockVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		return vec.isInAABB(min, max);
	}
	
	public void read(DataInput input) throws IOException
	{
		int x = input.readInt();
		int y = input.readInt();
		int z = input.readInt();
		
		min = new BlockVector(x, y, z);
		
		x = input.readInt();
		y = input.readInt();
		z = input.readInt();
		
		max = new BlockVector(x, y, z);
	}
	
	public void write(DataOutput output) throws IOException
	{
		output.writeInt(min.getBlockX());
		output.writeInt(min.getBlockY());
		output.writeInt(min.getBlockZ());
		
		output.writeInt(max.getBlockX());
		output.writeInt(max.getBlockY());
		output.writeInt(max.getBlockZ());
	}
}
