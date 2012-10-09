package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;
import java.util.HashMap;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.Pair;
import au.com.mineauz.PlayerSpy.Records.Record;

public class SearchResults 
{
	public HashMap<Integer, Cause> causes;
	public ArrayList<Pair<Record, Integer>> allRecords;
}
