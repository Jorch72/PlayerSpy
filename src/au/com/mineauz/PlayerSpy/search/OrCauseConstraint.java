package au.com.mineauz.PlayerSpy.search;

import java.util.List;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.search.interfaces.CauseConstraint;

public class OrCauseConstraint extends CauseConstraint
{
	private List<CauseConstraint> mConstraints;
	public OrCauseConstraint(List<CauseConstraint> constraints)
	{
		mConstraints = constraints;
	}

	@Override
	public boolean matches( Cause cause )
	{
		for(CauseConstraint constraint : mConstraints)
		{
			if(constraint.matches(cause))
				return true;
		}
		return false;
	}

	@Override
	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
