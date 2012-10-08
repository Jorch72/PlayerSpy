package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.entity.Entity;

import au.com.mineauz.PlayerSpy.StoredEntity;

public class DamageRecord extends Record 
{

	public DamageRecord(Entity damager, int amount) 
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
		stream.writeShort(mAmount);
		stream.writeBoolean(mDamager != null);
		
		if(mDamager != null)
		{
			mDamager.write(stream);
		}
	}
	
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mAmount = stream.readShort();
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
	
	public int getDamage()
	{
		return mAmount;
	}
	
	private StoredEntity mDamager;
	private int mAmount;
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 3 + (mDamager != null ? mDamager.getSize() : 0);
	}
}
