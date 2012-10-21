package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class RecordFilterDA extends DataAssembler
{

	@Override
	public Object assemble( ArrayDeque<Object> objects )
	{
		String target = (String)objects.pop();
		String type = (String)objects.pop();
		
		if(type.equals("hide"))
		{
			if(target.equals("blocks"))
			{
				objects.push(new RecordTypeConstraint(RecordType.Interact));
				objects.push(new RecordTypeConstraint(RecordType.ItemTransaction));
			}
			else if(target.equals("items"))
			{
				objects.push(new RecordTypeConstraint(RecordType.Interact));
				objects.push(new RecordTypeConstraint(RecordType.BlockChange));
			}
			else
			{
				objects.push(new RecordTypeConstraint(RecordType.BlockChange));
				objects.push(new RecordTypeConstraint(RecordType.ItemTransaction));
			}
		}
		else if(type.equals("only"))
		{
			if(target.equals("blocks"))
			{
				objects.push(new RecordTypeConstraint(RecordType.BlockChange));
			}
			else if(target.equals("items"))
			{
				objects.push(new RecordTypeConstraint(RecordType.ItemTransaction));
			}
			else
			{
				objects.push(new RecordTypeConstraint(RecordType.Interact));
			}
		}
		else
		{
			objects.push(new RecordTypeConstraint(RecordType.BlockChange));
			objects.push(new RecordTypeConstraint(RecordType.Interact));
			objects.push(new RecordTypeConstraint(RecordType.ItemTransaction));
		}
		return null;
	}

}
