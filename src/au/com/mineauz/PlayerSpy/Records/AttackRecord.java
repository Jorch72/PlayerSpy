package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import au.com.mineauz.PlayerSpy.storage.StoredEntity;

public class AttackRecord extends Record implements ILocationAware
{
	public AttackRecord(Entity damagee, double amount) 
	{
		super(RecordType.Attack);

		mDamagee = new StoredEntity(damagee);
		mAmount = amount;
	}
	public AttackRecord() 
	{
		super(RecordType.Attack);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeDouble(mAmount);
		
		mDamagee.write(stream);
	}
	
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		mAmount = stream.readDouble();

		mDamagee = new StoredEntity(); 
		mDamagee.read(stream);
	}

	public StoredEntity getDamagee()
	{
		return mDamagee;
	}
	
	/**
	 * Note: When this is equal to Integer.MAX_VALUE it means it was a kill
	 * @return
	 */
	public double getDamage()
	{
		return mAmount;
	}
	
	private StoredEntity mDamagee;
	private double mAmount;
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 8 + mDamagee.getSize();
	}
	@Override
	public String getDescription()
	{
		String entityName = mDamagee.getName();
		
		if(mDamagee.getEntityType().isAlive())
		{
			if(mAmount == -1)
				return ChatColor.DARK_AQUA + entityName + ChatColor.RESET + " was killed by %s";
			else
				return ChatColor.DARK_AQUA + entityName + ChatColor.RESET + " was damaged by %s";
		}
		else
		{
			if(mAmount == -1)
				return ChatColor.DARK_AQUA + entityName + ChatColor.RESET + " was broken by %s";
			else
				return ChatColor.DARK_AQUA + entityName + ChatColor.RESET + " was damaged by %s";
		}
	}
	@Override
	public Location getLocation()
	{
		return mDamagee.getLocation();
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof AttackRecord))
			return false;
		
		if(mAmount != ((AttackRecord)obj).mAmount)
			return false;
		
		if(!mDamagee.equals(((AttackRecord)obj).mDamagee))
			return false;
		
		return true;
	}
	
	@Override
	public String toString()
	{
		return "AttackRecord {target: " + mDamagee.getName() + " damage: " + mAmount + "}";
	}
}
