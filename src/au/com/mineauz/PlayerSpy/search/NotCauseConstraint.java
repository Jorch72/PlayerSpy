package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.search.interfaces.CauseConstraint;

public class NotCauseConstraint extends CauseConstraint
{
	private CauseConstraint mConstraint;
	
	public NotCauseConstraint(CauseConstraint constraint)
	{
		mConstraint = constraint;
	}
	@Override
	public boolean matches( Cause cause )
	{
		return !mConstraint.matches(cause);
	}

	@Override
	public String getDescription()
	{
		return null;
	}

}
