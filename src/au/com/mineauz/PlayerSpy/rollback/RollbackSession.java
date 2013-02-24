package au.com.mineauz.PlayerSpy.rollback;

import java.util.ArrayList;
import java.util.concurrent.Future;

import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.search.SearchFilter;
import au.com.mineauz.PlayerSpy.search.SearchResults;

public class RollbackSession
{
	public SearchFilter filter;
	public Player notifyPlayer;
	
	public boolean preview;
	
	public boolean restore;
	
	public Future<SearchResults> future;
	
	public SearchResults results = null;
	public int progress = 0;
	
	public int changed = 0;
	public int failed = 0;
	
	public ArrayList<Record> modified; 
}
