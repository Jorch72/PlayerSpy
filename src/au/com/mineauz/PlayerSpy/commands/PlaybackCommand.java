package au.com.mineauz.PlayerSpy.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.NullConversationPrefix;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.PlaybackContext;
import au.com.mineauz.PlayerSpy.PlaybackModeController;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Conversation.LogFilePrompt;

public class PlaybackCommand implements ICommand
{
	private ConversationFactory mConvoFactory;
	public PlaybackCommand()
	{
		mConvoFactory = new ConversationFactory(SpyPlugin.getInstance())
		.withModality(false)
		.withPrefix(new NullConversationPrefix())
		.withFirstPrompt(new LogFilePrompt())
		.withEscapeSequence("exit")
		.withEscapeSequence("quit")
		.withLocalEcho(false);
	}
	@Override
	public String getName() 
	{
		return "playback";
	}

	@Override
	public String[] getAliases() 
	{
		return new String[] { "play", "p" };
	}

	@Override
	public String getPermission() 
	{
		return "playerspy.playback";
	}

	@Override
	public String getUsageString(String label) 
	{
		return label + ChatColor.GREEN + " [player]";
	}

	@Override
	public boolean canBeConsole() {	return false; }

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length > 1)
			return false;
		
		if(SpyPlugin.getInstance().hasPlayback((Player)sender))
		{
			sender.sendMessage(ChatColor.RED + "You are already in playback mode.\n" + ChatColor.WHITE + " Please exit your current playback before starting a new one");
			return true;
		}
		
		if(args.length == 0)
			mConvoFactory.buildConversation((Conversable)sender).begin();
		else
		{
			PlaybackContext playback = SpyPlugin.getInstance().createPlayback((Player)sender, args[0]);

			if(playback != null)
			{
				SpyPlugin.getInstance().PlaybackChatControls.add(new PlaybackModeController((Player)sender, playback));
			}
		}
		
		return true;
	}

}
