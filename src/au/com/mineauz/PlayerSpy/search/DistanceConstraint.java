package au.com.mineauz.PlayerSpy.search;

import org.bukkit.Location;

import au.com.mineauz.PlayerSpy.Records.AttackRecord;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.ILocationAware;
import au.com.mineauz.PlayerSpy.Records.InteractRecord;
import au.com.mineauz.PlayerSpy.Records.InventoryTransactionRecord;
import au.com.mineauz.PlayerSpy.Records.PaintingChangeRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.VehicleMountRecord;

public class DistanceConstraint extends Constraint
{
	public double distance;
	public Location location;

	public DistanceConstraint()
	{
		
	}
	public DistanceConstraint(double distance, Location location)
	{
		this.distance = distance;
		this.location = location.clone();
	}
	@Override
	public boolean matches( Record record )
	{
		if(record instanceof ILocationAware)
		{
			Location other = ((ILocationAware)record).getLocation();
			if(location.getWorld().equals(other.getWorld()))
			{
				if(location.distanceSquared(other) <= (distance * distance))
					return true;
			}
		}
		else if(record instanceof BlockChangeRecord)
		{
			Location other = ((BlockChangeRecord)record).getLocation();
			if(location.getWorld().equals(other.getWorld()))
			{
				if(location.distanceSquared(other) <= (distance * distance))
					return true;
			}
		}
		else if(record instanceof AttackRecord)
		{
			Location other = ((AttackRecord)record).getDamagee().getLocation();
			if(location.getWorld().equals(other.getWorld()))
			{
				if(location.distanceSquared(other) <= (distance * distance))
					return true;
			}
		}
		else if(record instanceof PaintingChangeRecord)
		{
			Location other = ((PaintingChangeRecord)record).getPainting().getLocation();
			if(location.getWorld().equals(other.getWorld()))
			{
				if(location.distanceSquared(other) <= (distance * distance))
					return true;
			}
		}
		else if(record instanceof VehicleMountRecord)
		{
			Location other = ((VehicleMountRecord)record).getVehicle().getLocation();
			if(location.getWorld().equals(other.getWorld()))
			{
				if(location.distanceSquared(other) <= (distance * distance))
					return true;
			}
		}
		else if(record instanceof InteractRecord)
		{
			InteractRecord irecord = (InteractRecord)record;
			
			if(irecord.hasBlock())
			{
				Location other = irecord.getBlock().getLocation();
				if(location.getWorld().equals(other.getWorld()))
				{
					if(location.distanceSquared(other) <= (distance * distance))
						return true;
				}
			}
			else if(irecord.hasEntity())
			{
				Location other = irecord.getEntity().getLocation();
				if(location.getWorld().equals(other.getWorld()))
				{
					if(location.distanceSquared(other) <= (distance * distance))
						return true;
				}
			}
		}
		else if(record instanceof InventoryTransactionRecord)
		{
			InventoryTransactionRecord irecord = (InventoryTransactionRecord)record;
			
			if(irecord.getInventoryInfo().getBlock() != null)
			{
				Location other = irecord.getInventoryInfo().getBlock().getLocation();
				if(location.getWorld().equals(other.getWorld()))
				{
					if(location.distanceSquared(other) <= (distance * distance))
						return true;
				}
			}
			else if(irecord.getInventoryInfo().getEntity() != null)
			{
				Location other = irecord.getInventoryInfo().getEntity().getLocation();
				if(location.getWorld().equals(other.getWorld()))
				{
					if(location.distanceSquared(other) <= (distance * distance))
						return true;
				}
			}
		}
		return false;
	}
}
