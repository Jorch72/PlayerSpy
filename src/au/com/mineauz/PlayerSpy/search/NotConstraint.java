package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.search.interfaces.IConstraint;

public class NotConstraint<T> implements IConstraint<T>
{
	private IConstraint<T> mConstraint;
	public NotConstraint(IConstraint<T> constraint)
	{
		if(constraint == null)
			throw new IllegalArgumentException("Constraint can not be null");
		
		mConstraint = constraint;
	}
	@Override
	public boolean matches( T item )
	{
		return !mConstraint.matches(item);
	}

	@Override
	public String getDescription()
	{
		return "not " + mConstraint.getDescription();
	}

}
