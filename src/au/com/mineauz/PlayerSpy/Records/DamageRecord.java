package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import au.com.mineauz.PlayerSpy.storage.StoredEntity;

public class DamageRecord extends Record implements ILocationAware
{

	public DamageRecord(Entity damager, double amount) 
	{
		super(RecordType.Damage);
		if(damager != null)
			mDamager = new StoredEntity(damager);
		
		mAmount = amount;
	}
	public DamageRecord()
	{
		super(RecordType.Damage);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeDouble(mAmount);
		stream.writeBoolean(mDamager != null);
		
		if(mDamager != null)
		{
			mDamager.write(stream);
		}
	}
	
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		mAmount = stream.readDouble();
		boolean hasDamager = stream.readBoolean();
		
		if(hasDamager)
		{
			mDamager = new StoredEntity();
			mDamager.read(stream);
		}
	}
	
	public StoredEntity getDamager()
	{
		return mDamager;
	}
	
	public double getDamage()
	{
		return mAmount;
	}
	
	private StoredEntity mDamager;
	private double mAmount;
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 5 + (mDamager != null ? mDamager.getSize() : 0);
	}
	@Override
	public String getDescription()
	{
		if(mDamager != null)
		{
			String entityName = (mDamager.getEntityType() == EntityType.PLAYER ? mDamager.getPlayerName() : mDamager.getEntityType().getName());
			return "%s took " + mAmount + " damage from " + ChatColor.DARK_AQUA + entityName + ChatColor.RESET;
		}
		else
		{
			return "%s took " + mAmount + " damage";
		}
	}
	@Override
	public Location getLocation()
	{
		if(mDamager != null)
			return mDamager.getLocation();
		else
			return null;
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof DamageRecord))
			return false;
		
		DamageRecord record = (DamageRecord)obj;
		
		return ((mDamager == null && record.mDamager == null) || (mDamager != null && mDamager.equals(record.mDamager))) && mAmount == record.mAmount;
	}
}
