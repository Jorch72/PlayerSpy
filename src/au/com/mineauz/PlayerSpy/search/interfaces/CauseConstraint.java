package au.com.mineauz.PlayerSpy.search.interfaces;

import au.com.mineauz.PlayerSpy.Cause;

public abstract class CauseConstraint implements IConstraint<Cause>
{
	@Override
	public abstract boolean matches( Cause cause );
	
	@Override
	public abstract String getDescription();
}
