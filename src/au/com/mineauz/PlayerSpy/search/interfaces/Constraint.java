package au.com.mineauz.PlayerSpy.search.interfaces;

import au.com.mineauz.PlayerSpy.Records.Record;

public abstract class Constraint 
{
	public abstract boolean matches(Record record);
	
	public abstract String getDescription();
	
}
