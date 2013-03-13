package au.com.mineauz.PlayerSpy.search;

import java.util.Arrays;
import java.util.List;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class CompoundConstraint extends Constraint
{
	private List<Constraint> mSubConstraints;
	private boolean mAnd;
	
	public CompoundConstraint(boolean and, Constraint...constraints)
	{
		if(constraints == null || constraints.length == 0)
			throw new IllegalArgumentException("Must provide at least 1 constraint");
		
		mAnd = and;
		mSubConstraints = Arrays.asList(constraints);
	}
	
	public CompoundConstraint(boolean and, List<Constraint> constraints)
	{
		if(constraints == null || constraints.isEmpty())
			throw new IllegalArgumentException("Must provide at least 1 constraint");
		
		mAnd = and;
		mSubConstraints = constraints;
	}
	@Override
	public boolean matches( Record record )
	{
		boolean ok = mAnd;
		for(Constraint c : mSubConstraints)
		{
			if(mAnd)
				ok = ok && c.matches(record);
			else
				ok = ok || c.matches(record);
			
			if(mAnd == !ok)
				break;
		}
		
		return ok;
	}

	@Override
	public String getDescription()
	{
		return null;
	}

}
