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
		String list = "";
		int count = 0;
		for(CauseConstraint constraint : mConstraints)
		{
			String str = constraint.getDescription();
			if(str != null)
			{
				if(!list.isEmpty())
					list += " or ";
				
				list += str;
				count++;
			}
		}
		
		if(count > 1)
			return "( " + list + " )";
		return list;
	}

}
