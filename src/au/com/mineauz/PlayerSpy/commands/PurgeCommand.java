package au.com.mineauz.PlayerSpy.commands;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Util;

public class PurgeCommand implements ICommand
{
	@Override
	public String getName() 
	{
		return "purge";
	}

	@Override
	public String[] getAliases() 
	{
		return null;
	}

	@Override
	public String getPermission() 
	{
		return "playerspy.purge";
	}

	@Override
	public String getUsageString(String label) 
	{
		return label + ChatColor.GOLD + " <player> " + ChatColor.GREEN + "[before <datetime> | after <datetime> | between <datetime> <datetime>]";
	}

	@Override
	public boolean canBeConsole() { return true; }

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length == 0 || args.length == 2)
			return false;
		
		String playerName = args[0];
		
		long startDate = 0;
		long endDate = Calendar.getInstance().getTimeInMillis();
		
		// Date modifiers
		if(args.length > 2)
		{
			if(args[1].equalsIgnoreCase("before"))
			{
				DateFormat fmt = DateFormat.getDateTimeInstance();
				String temp = "";
				boolean success = false;
				for(int i = 2; i < args.length; ++i)
				{
					temp += (i != 2 ? " " : "") + args[i];
					try
					{
						startDate = fmt.parse(temp).getTime();
						success = true;
						break;
					}
					catch(ParseException e)
					{
						// Keep going
					}
				}
				
				if(!success)
				{
					sender.sendMessage(ChatColor.RED + "Invalid date format");
					return true;
				}
			}
			else if(args[1].equalsIgnoreCase("after"))
			{
				DateFormat fmt = DateFormat.getDateTimeInstance();
				String temp = "";
				boolean success = false;
				for(int i = 2; i < args.length; ++i)
				{
					temp += (i != 2 ? " " : "") + args[i];
					try
					{
						endDate = fmt.parse(temp).getTime();
						success = true;
						break;
					}
					catch(ParseException e)
					{
						// Keep going
					}
				}
				
				if(!success)
				{
					sender.sendMessage(ChatColor.RED + "Invalid date format");
					return true;
				}
			}
			else if(args[1].equalsIgnoreCase("between"))
			{
				DateFormat fmt = DateFormat.getDateTimeInstance();
				String temp = "";
				boolean success = false;
				int i;
				for(i = 2; i < args.length; ++i)
				{
					temp += (i != 2 ? " " : "") + args[i];
					try
					{
						startDate = fmt.parse(temp).getTime();
						success = true;
						break;
					}
					catch(ParseException e)
					{
						// Keep going
					}
				}
				
				if(!success)
				{
					sender.sendMessage(ChatColor.RED + "Invalid date format");
					return true;
				}
				
				success = false;
				temp = "";
				int start = i;
				for(; i < args.length; ++i)
				{
					temp += (i != start ? " " : "") + args[i];
					try
					{
						endDate = fmt.parse(temp).getTime();
						success = true;
						break;
					}
					catch(ParseException e)
					{
						// Keep going
					}
				}
				if(!success)
				{
					sender.sendMessage(ChatColor.RED + "Invalid date format");
					return true;
				}
			}
		}
		
		LogFile log = SpyPlugin.getInstance().loadLog(playerName);
		
		if(log == null)
		{
			sender.sendMessage(ChatColor.RED + "There is no data for " + playerName);
			return true;
		}
		
		if((startDate < log.getStartDate() && endDate < log.getStartDate()) || (startDate > log.getEndDate() && endDate > log.getEndDate()))
		{
			sender.sendMessage(ChatColor.RED + "There is no data to purge there");
			return true;
		}
		
		// Ask for confirm
		HashMap<Object,Object> sdata = new HashMap<Object, Object>();
		sdata.put("log", log);
		sdata.put("from", startDate);
		sdata.put("to", endDate);
		
		ConversationFactory factory = new ConversationFactory(SpyPlugin.getInstance())
			.withTimeout(10)
			.withLocalEcho(false)
			.withModality(false)
			.withFirstPrompt(new ConfirmPrompt())
			.withInitialSessionData(sdata);
		
		factory.buildConversation((Conversable)sender).begin();
		
		return true;
	}
	private class ConfirmPrompt extends StringPrompt
	{
		@Override
		public String getPromptText(ConversationContext context) 
		{
			LogFile log = (LogFile)context.getSessionData("log");
			long start = (Long)context.getSessionData("from");
			long end = (Long)context.getSessionData("to");
			
			if(start >= log.getStartDate() && start < log.getEndDate() && end >= log.getStartDate() && end < log.getEndDate())
				return "Are you sure you want to purge all data for " + ChatColor.YELLOW + log.getName() + ChatColor.RESET + " between " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + " and " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + "? (type yes or no)";
			else if(start >= log.getStartDate() && start < log.getEndDate())
				return "Are you sure you want to purge all data for " + ChatColor.YELLOW + log.getName() + ChatColor.RESET + " from " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + "? (type yes or no)";
			else if(end >= log.getStartDate() && end < log.getEndDate())
				return "Are you sure you want to purge all data for " + ChatColor.YELLOW + log.getName() + ChatColor.RESET + " before " + ChatColor.GREEN + Util.dateToString(end) + ChatColor.RESET + "? (type yes or no)";
			else
				return "Are you sure you want to purge all data for " + ChatColor.YELLOW + log.getName() + ChatColor.RESET + "? (type yes or no)";
		}

		@Override
		public Prompt acceptInput(ConversationContext context, String input) 
		{
			if(input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y"))
			{
				LogFile log = (LogFile)context.getSessionData("log");
				long start = (Long)context.getSessionData("from");
				long end = (Long)context.getSessionData("to");
				
				if(start >= log.getStartDate() && start < log.getEndDate() && end >= log.getStartDate() && end < log.getEndDate())
					((CommandSender)context.getForWhom()).sendMessage("Now purging all data for " + ChatColor.YELLOW + (String)context.getSessionData("player") + ChatColor.RESET + " between " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + " and " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + "? (type yes or no)");
				else if(start >= log.getStartDate() && start < log.getEndDate())
					((CommandSender)context.getForWhom()).sendMessage("Now purging all data for " + ChatColor.YELLOW + (String)context.getSessionData("player") + ChatColor.RESET + " from " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + "? (type yes or no)");
				else if(end >= log.getStartDate() && end < log.getEndDate())
					((CommandSender)context.getForWhom()).sendMessage("Now purging all data for " + ChatColor.YELLOW + (String)context.getSessionData("player") + ChatColor.RESET + " before " + ChatColor.GREEN + Util.dateToString(end) + ChatColor.RESET + "? (type yes or no)");
				else
					((CommandSender)context.getForWhom()).sendMessage("Now purging all data for " + ChatColor.YELLOW + (String)context.getSessionData("player") + ChatColor.RESET + "? (type yes or no)");
				
				log.purgeRecordsAsync(start, end);
				
				return Prompt.END_OF_CONVERSATION;
			}
			else if(input.equalsIgnoreCase("no") || input.equalsIgnoreCase("n"))
			{
				LogFile log = (LogFile)context.getSessionData("log");
				log.closeAsync(true);
				return Prompt.END_OF_CONVERSATION;
			}
			else
				return this;
		}
		
	}
}
