package au.com.mineauz.PlayerSpy.Conversation;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.*;

public class LogFilePrompt extends ValidatingPrompt
{
	@Override
	public String getPromptText(ConversationContext context) 
	{
		return "What player should be played back?";
	}

	@Override
	protected boolean isInputValid(ConversationContext context, String input) 
	{
		String sanitisedName = input;
		sanitisedName = sanitisedName.replace(':', '_');
		sanitisedName = sanitisedName.replace('/', '_');
		sanitisedName = sanitisedName.replace('\\', '_');
		sanitisedName = sanitisedName.replace('%', '_');
		sanitisedName = sanitisedName.replace('.', '_');
		
		if(new File(context.getPlugin().getDataFolder(),sanitisedName + ".trackdata").exists())
			return true;
		else
			return false;
	}
	@Override
	protected Prompt acceptValidatedInput(ConversationContext context, String input) 
	{
		Player viewer = ((Player)context.getForWhom());
		
		PlaybackContext playback = ((SpyPlugin)context.getPlugin()).createPlayback(viewer, input);

		if(playback != null)
		{
			SpyPlugin.getInstance().PlaybackChatControls.add(new PlaybackModeController(viewer, playback));
		}
		return Prompt.END_OF_CONVERSATION;
	}
	
	@Override
	protected String getFailedValidationText(ConversationContext context, String invalidInput) 
	{
		return "There is no log file for " + ChatColor.YELLOW + invalidInput;
	}
}
