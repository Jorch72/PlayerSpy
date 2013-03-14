package au.com.mineauz.PlayerSpy.search.interfaces;

import au.com.mineauz.PlayerSpy.Records.Record;

public abstract class Constraint implements IConstraint<Record>
{
	@Override
	public abstract boolean matches(Record record);
	
	@Override
	public abstract String getDescription();
	
}
