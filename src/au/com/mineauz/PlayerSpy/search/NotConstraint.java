package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class NotConstraint extends Constraint
{
	private Constraint mConstraint;
	public NotConstraint(Constraint constraint)
	{
		if(constraint == null)
			throw new IllegalArgumentException("Constraint can not be null");
		
		mConstraint = constraint;
	}
	@Override
	public boolean matches( Record record )
	{
		return !mConstraint.matches(record);
	}

	@Override
	public String getDescription()
	{
		return null;
	}

}
