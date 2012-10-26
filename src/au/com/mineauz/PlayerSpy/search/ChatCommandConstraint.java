package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Records.ChatCommandRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class ChatCommandConstraint extends Constraint
{
	public boolean command;
	public String contains;
	
	@Override
	public String toString() 
	{
		return "{ isCommand: " + command + (contains != null ? ", must contain: '" + contains + "'" : "") + " }";
	}

	@Override
	public boolean matches( Record record )
	{
		if(record.getType() != RecordType.ChatCommand)
			return false;
		
		if(command)
		{
			if(!((ChatCommandRecord)record).getMessage().startsWith("/"))
				return false;
		}
		else
		{
			if(((ChatCommandRecord)record).getMessage().startsWith("/"))
				return false;
		}
		
		if(contains == null || ((ChatCommandRecord)record).getMessage().toLowerCase().contains(contains.toLowerCase()))
			return true;
		
		return false;
	}
}
