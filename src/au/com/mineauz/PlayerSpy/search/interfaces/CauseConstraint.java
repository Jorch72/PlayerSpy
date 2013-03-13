package au.com.mineauz.PlayerSpy.search.interfaces;

import au.com.mineauz.PlayerSpy.Cause;

public abstract class CauseConstraint
{
	public abstract boolean matches( Cause cause );
	
	public abstract String getDescription();
}
