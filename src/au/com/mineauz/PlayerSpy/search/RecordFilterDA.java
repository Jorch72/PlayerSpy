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
				objects.push(new RecordTypeConstraint(RecordType.ItemPickup));
			}
			else if(target.equals("items"))
			{
				objects.push(new RecordTypeConstraint(RecordType.Interact));
				objects.push(new RecordTypeConstraint(RecordType.BlockChange));
				objects.push(new RecordTypeConstraint(RecordType.PaintingChange));
			}
			else
			{
				objects.push(new RecordTypeConstraint(RecordType.BlockChange));
				objects.push(new RecordTypeConstraint(RecordType.ItemTransaction));
				objects.push(new RecordTypeConstraint(RecordType.PaintingChange));
				objects.push(new RecordTypeConstraint(RecordType.ItemPickup));
			}
		}
		else if(type.equals("only"))
		{
			if(target.equals("blocks"))
			{
				objects.push(new RecordTypeConstraint(RecordType.BlockChange));
				objects.push(new RecordTypeConstraint(RecordType.PaintingChange));
			}
			else if(target.equals("items"))
			{
				objects.push(new RecordTypeConstraint(RecordType.ItemTransaction));
				objects.push(new RecordTypeConstraint(RecordType.ItemPickup));
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
			objects.push(new RecordTypeConstraint(RecordType.PaintingChange));
		}
		return null;
	}

}
