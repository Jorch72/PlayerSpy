package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Records.IRollbackable;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class RolledbackConstraint extends Constraint
{
	private boolean mIsRolledBack;
	
	public RolledbackConstraint(boolean isRolledBack)
	{
		mIsRolledBack = isRolledBack;
	}
	
	@Override
	public boolean matches( Record record )
	{
		if(record instanceof IRollbackable)
		{
			return ((IRollbackable)record).wasRolledBack() == mIsRolledBack;
		}
		return true;
	}

	@Override
	public String getDescription()
	{
		return null;
	}

}
