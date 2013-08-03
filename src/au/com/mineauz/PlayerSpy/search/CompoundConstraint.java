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
	
	public List<Constraint> getSubContraints()
	{
		return mSubConstraints;
	}

	@Override
	public String getDescription()
	{
		String sep = mAnd ? "and" : "or";
		
		String list = "";
		
		int count = 0;
		for(Constraint c : mSubConstraints)
		{
			String str = c.getDescription();
			if(str != null)
			{
				if(!list.isEmpty())
					list += " " + sep + " ";
				
				list += str;
				++count;
			}
		}
		
		if(count > 1)
			return "( " + list + " )";
		return list;
	}

}
