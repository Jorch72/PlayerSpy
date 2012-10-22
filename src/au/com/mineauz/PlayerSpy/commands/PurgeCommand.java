package au.com.mineauz.PlayerSpy.commands;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.LogTasks.PurgeTask;
import au.com.mineauz.PlayerSpy.Utilities.Util;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;

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
		return label + ChatColor.GOLD + " (<player>|<world>|all) " + ChatColor.GREEN + "[before <datetime> | after <datetime> | between <datetime> <datetime>]";
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
		
		HashMap<Object,Object> sdata = new HashMap<Object, Object>();
		
		
		// Date modifiers
		if(args.length > 2)
		{
			if(args[1].equalsIgnoreCase("before"))
			{
				String temp = "";
				boolean success = false;
				for(int i = 2; i < args.length; ++i)
				{
					SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
					fmt.setTimeZone(SpyPlugin.getSettings().timezone);
					
					temp += (i != 2 ? " " : "") + args[i];
					try
					{
						endDate = fmt.parse(temp).getTime();
						sdata.put("to", endDate);
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
				SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
				fmt.setTimeZone(SpyPlugin.getSettings().timezone);
				
				String temp = "";
				boolean success = false;
				for(int i = 2; i < args.length; ++i)
				{
					temp += (i != 2 ? " " : "") + args[i];
					try
					{
						startDate = fmt.parse(temp).getTime();
						sdata.put("from", startDate);

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
				SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
				fmt.setTimeZone(SpyPlugin.getSettings().timezone);
				
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
						sdata.put("from", startDate);
						sdata.put("to", endDate);
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
		

		boolean allLogs = false;
		if(playerName.equalsIgnoreCase("all"))
		{
			allLogs = true;
		}
		else 
		{
			if(!LogFileRegistry.hasLogFile(Bukkit.getWorld(playerName)))
			{
				if(!LogFileRegistry.hasLogFile(Bukkit.getOfflinePlayer(playerName)))
				{
					return false;
				}
			}
			else
				playerName = LogFileRegistry.cGlobalFilePrefix + playerName;
		}
		
		// Ask for confirm
		sdata.put("owner", playerName.toLowerCase());
		sdata.put("all", allLogs);
		
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
			String owner = (String)context.getSessionData("owner");
			boolean allLogs = (Boolean)context.getSessionData("all");
			Long start = (Long)context.getSessionData("from");
			Long end = (Long)context.getSessionData("to");
			
			if(allLogs)
			{
				if(start != null && end != null)
					return "Are you sure you want to purge " + ChatColor.BOLD + "all" + ChatColor.RESET + " data between " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + " and " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + "? (type yes or no)";
				else if(start != null)
					return "Are you sure you want to purge " + ChatColor.BOLD + "all" + ChatColor.RESET + " data after " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + "? (type yes or no)";
				else if(end != null)
					return "Are you sure you want to purge " + ChatColor.BOLD + "all" + ChatColor.RESET + " data before " + ChatColor.GREEN + Util.dateToString(end) + ChatColor.RESET + "? (type yes or no)";
				else
					return "Are you sure you want to purge " + ChatColor.BOLD + "all" + ChatColor.RESET + " data? (type yes or no)";
			}
			else
			{
				if(start != null && end != null)
					return "Are you sure you want to purge all data for " + ChatColor.YELLOW + owner + ChatColor.RESET + " between " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + " and " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + "? (type yes or no)";
				else if(start != null)
					return "Are you sure you want to purge all data for " + ChatColor.YELLOW + owner + ChatColor.RESET + " after " + ChatColor.GREEN + Util.dateToString(start) + ChatColor.RESET + "? (type yes or no)";
				else if(end != null)
					return "Are you sure you want to purge all data for " + ChatColor.YELLOW + owner + ChatColor.RESET + " before " + ChatColor.GREEN + Util.dateToString(end) + ChatColor.RESET + "? (type yes or no)";
				else
					return "Are you sure you want to purge all data for " + ChatColor.YELLOW + owner + ChatColor.RESET + "? (type yes or no)";
			}
		}

		@Override
		public Prompt acceptInput(ConversationContext context, String input) 
		{
			if(input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y"))
			{
				String owner = (String)context.getSessionData("owner");
				boolean allLogs = (Boolean)context.getSessionData("all");
				Long start = (Long)context.getSessionData("from");
				Long end = (Long)context.getSessionData("to");

				if(start == null)
					start = 0L;
				if(end == null)
					end = Long.MAX_VALUE;
				
				PurgeTask task = new PurgeTask((CommandSender)context.getForWhom(), owner, allLogs, start,end);
				SpyPlugin.getExecutor().submit(task);
				
				return Prompt.END_OF_CONVERSATION;
			}
			else if(input.equalsIgnoreCase("no") || input.equalsIgnoreCase("n"))
			{
				return Prompt.END_OF_CONVERSATION;
			}
			else
				return this;
		}
		
	}
	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}
}
