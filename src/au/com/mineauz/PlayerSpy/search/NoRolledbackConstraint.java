package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Records.IRollbackable;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class NoRolledbackConstraint extends Constraint
{

	@Override
	public boolean matches( Record record )
	{
		if(record instanceof IRollbackable)
		{
			return !((IRollbackable)record).wasRolledBack();
		}
		return true;
	}

	@Override
	public String getDescription()
	{
		return "No rolled back records";
	}

}
