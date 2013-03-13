package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Records.AttackRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;

public class AttackConstraint extends RecordTypeConstraint
{
	private boolean mKill;
	public AttackConstraint(boolean kill)
	{
		super(RecordType.Attack);
		mKill = kill;
	}
	
	@Override
	public boolean matches( Record record )
	{
		if(!super.matches(record))
			return false;
		
		if(mKill && ((AttackRecord)record).getDamage() == -1)
			return true;
		else if(!mKill && ((AttackRecord)record).getDamage() != -1)
			return true;
		
		return false;
	}
}
