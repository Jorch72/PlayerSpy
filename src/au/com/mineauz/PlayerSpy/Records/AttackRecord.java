package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.entity.Entity;

import au.com.mineauz.PlayerSpy.StoredEntity;

public class AttackRecord extends Record 
{
	public AttackRecord(Entity damagee, int amount) 
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
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		stream.writeShort(mAmount);
		
		mDamagee.write(stream);
	}
	
	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mAmount = stream.readShort();

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
	public int getDamage()
	{
		return mAmount;
	}
	
	private StoredEntity mDamagee;
	private int mAmount;
	@Override
	protected int getContentSize() 
	{
		return 2 + mDamagee.getSize();
	}
	
	
}
