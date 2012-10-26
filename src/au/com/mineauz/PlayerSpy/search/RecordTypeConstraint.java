package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class RecordTypeConstraint extends Constraint
{
	public RecordTypeConstraint(RecordType type)
	{
		this.type = type;
	}
	public RecordType type;
	
	@Override
	public boolean matches( Record record )
	{
		return (record.getType() == type);
	}

}
