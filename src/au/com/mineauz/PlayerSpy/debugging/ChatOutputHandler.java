package au.com.mineauz.PlayerSpy.debugging;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.bukkit.entity.Player;

public class ChatOutputHandler extends Handler
{
	private HashMap<Player, Level> mDebugReceivers;
	private ChatOutputFormatter mFormat = new ChatOutputFormatter();
	
	public ChatOutputHandler(HashMap<Player, Level> receivers)
	{
		mDebugReceivers = receivers;
	}
	
	@Override
	public void publish( LogRecord record )
	{
		for(Entry<Player, Level> player : mDebugReceivers.entrySet())
		{
			if(record.getLevel().intValue() >= player.getValue().intValue())
				player.getKey().sendMessage(mFormat.format(record));
		}
	}

	@Override
	public void flush()
	{
	}

	@Override
	public void close() throws SecurityException
	{
	}

}
