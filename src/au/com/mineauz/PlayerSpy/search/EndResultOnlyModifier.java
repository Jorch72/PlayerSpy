package au.com.mineauz.PlayerSpy.search;

import java.util.HashSet;

import org.bukkit.Location;

import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.search.interfaces.FormatterModifier;

public class EndResultOnlyModifier implements FormatterModifier
{

	
	
	@Override
	public void format( SearchResults results )
	{
		HashSet<Location> blocks = new HashSet<Location>();
		
		
		for(int i = results.allRecords.size() - 1; i >= 0 ; i--)
		{
			Record record = results.allRecords.get(i).getArg1();
			
			if(record instanceof BlockChangeRecord)
			{
				if(blocks.contains(((BlockChangeRecord) record).getLocation()))
				{
					results.allRecords.remove(i);
					continue;
				}
				
				blocks.add(((BlockChangeRecord) record).getLocation());
			}
		}

	}

}
