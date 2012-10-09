package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;


public class PlayerNameState extends State 
{
	@Override
	public boolean match(String word, ArrayDeque<Object> output) 
	{
		// Try short online names
		OfflinePlayer player = Bukkit.getPlayer(word);
		if(player == null)
			player = Bukkit.getOfflinePlayer(word);
		
		if(!player.hasPlayedBefore())
			return false;
		
		output.push(player);
		return true;
	}

	@Override
	public String getExpected() 
	{
		return "player name";
	}

}
