package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Records.ChatCommandRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;

public class ChatConstraint extends RecordTypeConstraint
{
	public ChatConstraint()
	{
		super(RecordType.ChatCommand);
	}
	
	@Override
	public boolean matches( Record record )
	{
		if(!super.matches(record))
			return false;
		
		if(((ChatCommandRecord)record).getMessage().startsWith("/"))
			return false;
		
		return true;
	}
	
	@Override
	public String getDescription()
	{
		return "Chat";
	}
}
