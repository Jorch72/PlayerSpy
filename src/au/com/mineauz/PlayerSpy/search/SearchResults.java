package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;
import java.util.HashMap;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Pair;

public class SearchResults 
{
	public SearchFilter usedFilter;
	public HashMap<Integer, Cause> causes;
	public ArrayList<Pair<Record, Integer>> allRecords;
}
