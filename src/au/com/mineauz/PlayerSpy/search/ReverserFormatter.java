package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.search.interfaces.FormatterModifier;

public class ReverserFormatter implements FormatterModifier
{
	@Override
	public void format( SearchResults results )
	{
		ArrayList<Pair<Record, Integer>> newList = new ArrayList<Pair<Record, Integer>>();
		
		for(int i = results.allRecords.size()-1; i >= 0; --i )
			newList.add(results.allRecords.get(i));
		results.allRecords = newList;
	}

}
