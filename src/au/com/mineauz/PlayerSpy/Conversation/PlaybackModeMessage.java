package au.com.mineauz.PlayerSpy.Conversation;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;

public class PlaybackModeMessage extends MessagePrompt
{

	@Override
	public String getPromptText(ConversationContext context) 
	{
		return "You are now in " + ChatColor.AQUA + "Playback Mode" + ChatColor.WHITE + ". You cannot use non playback commands.\nTo exit: type " + ChatColor.YELLOW + "exit" + ChatColor.WHITE + " or "  + ChatColor.YELLOW + "quit";
	}

	@Override
	protected Prompt getNextPrompt(ConversationContext context) 
	{
		return new LogFilePrompt();
	}

}
