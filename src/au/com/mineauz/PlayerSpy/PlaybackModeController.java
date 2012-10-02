package au.com.mineauz.PlayerSpy;

import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import au.com.mineauz.PlayerSpy.commands.Command;
import au.com.mineauz.PlayerSpy.commands.playback.FindCommand;
import au.com.mineauz.PlayerSpy.commands.playback.HelpCommand;
import au.com.mineauz.PlayerSpy.commands.playback.InfoCommand;
import au.com.mineauz.PlayerSpy.commands.playback.PauseCommand;
import au.com.mineauz.PlayerSpy.commands.playback.PlayCommand;
import au.com.mineauz.PlayerSpy.commands.playback.SeeInvCommand;
import au.com.mineauz.PlayerSpy.commands.playback.SeekCommand;
import au.com.mineauz.PlayerSpy.commands.playback.SkipCommand;
import au.com.mineauz.PlayerSpy.commands.playback.TpCommand;
import au.com.mineauz.PlayerSpy.commands.playback.ViewerCommand;

public class PlaybackModeController implements Listener 
{
	private Player mPlayer;
	private PlaybackContext mPlayback;
	private static HashMap<String,Command> sCommandHandlers = new HashMap<String,Command>();
	
	static 
	{
		sCommandHandlers.put("play", new PlayCommand());
		sCommandHandlers.put("pause", new PauseCommand());
		sCommandHandlers.put("skip", new SkipCommand());
		sCommandHandlers.put("seek", new SeekCommand());
		sCommandHandlers.put("seeinv", new SeeInvCommand());
		sCommandHandlers.put("find", new FindCommand());
		sCommandHandlers.put("tp", new TpCommand());
		sCommandHandlers.put("viewer", new ViewerCommand());
		sCommandHandlers.put("info", new InfoCommand());
		sCommandHandlers.put("help", new HelpCommand(sCommandHandlers));
	}
	
	public PlaybackModeController(Player player, PlaybackContext playback)
	{
		mPlayer = player;
		mPlayback = playback;
		
		player.sendMessage(ChatColor.YELLOW + playback.getTargetName(0) + ChatColor.WHITE + " has been loaded. Use " + ChatColor.YELLOW + "exit" + ChatColor.WHITE + " to leave playback mode\nUse " + ChatColor.YELLOW + "seek" + ChatColor.WHITE + " or " + ChatColor.YELLOW + "find" + ChatColor.WHITE + " to set the starting time\nYou can also prefix your command with ! to turn it into regular chat");
		SpyPlugin.getInstance().getServer().getPluginManager().registerEvents(this, SpyPlugin.getInstance());
	}
	
	@EventHandler
	private void onPlayerChatEvent(AsyncPlayerChatEvent event)
	{
		if(mPlayer == null)
			return;
		
		if(event.getPlayer() == mPlayer)
		{
			// Prefixing with ! allows you to chat
			if(event.getMessage().startsWith("!"))
				event.setMessage(event.getMessage().substring(1));
			else
			{
				event.setCancelled(true);
				
				String[] splitStr = event.getMessage().toLowerCase().split("\\s+");
				
				if(splitStr.length == 0)
					return;
				
				String[] args = Arrays.copyOfRange(splitStr, 1, splitStr.length);
				
				String command = splitStr[0];
				
				if(command.equals("exit") || command.equals("quit"))
				{
					SpyPlugin.getInstance().removePlayback(mPlayer);
					mPlayer.sendMessage("You are no longer in playback mode");
					mPlayer = null;
				}
				else if(sCommandHandlers.containsKey(command))
				{
					sCommandHandlers.get(command).onCommand(mPlayer, mPlayback, args);
				}
				else
				{
					mPlayer.sendMessage(ChatColor.RED + "Invalid command. Type " + ChatColor.YELLOW + "help" + ChatColor.RED + " for a list of commands.");
				}
			}
		}
	}
	
	@EventHandler
	private void onPlayerQuit(PlayerQuitEvent event)
	{
		if(mPlayer == null)
			return;
		
		if(event.getPlayer() == mPlayer)
		{
			// Terminate playback
			SpyPlugin.getInstance().removePlayback(mPlayer);
			mPlayer = null;
			
		}
	}
	
}
