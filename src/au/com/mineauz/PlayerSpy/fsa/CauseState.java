package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import au.com.mineauz.PlayerSpy.Cause;

public class CauseState extends State
{

	@Override
	public boolean match( String word, ArrayDeque<Object> output )
	{
		if(word.contains(">"))
		{
			String name, extra;
			name = word.substring(0,word.indexOf(">"));
			extra = word.substring(word.indexOf(">") + 1);
			
			OfflinePlayer player = Bukkit.getOfflinePlayer(name);
			if(!player.hasPlayedBefore())
				return false;
			
			Cause c = Cause.playerCause(player, extra);
			output.push(c);
		}
		else if(word.startsWith("#"))
		{
			Cause c = Cause.globalCause(Bukkit.getWorlds().get(0), word.substring(1));
			output.push(c);
		}
		else
		{
			OfflinePlayer player = Bukkit.getOfflinePlayer(word);
			if(!player.hasPlayedBefore())
				return false;
			
			Cause c = Cause.playerCause(player);
			output.push(c);
		}
		
		return true;
	}

	@Override
	public String getExpected()
	{
		return "cause";
	}

}
