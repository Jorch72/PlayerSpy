package au.com.mineauz.PlayerSpy.commands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

/**
 * This allows sub commands to be handled in a clean easily expandable way.
 * Just create a new command that implements ICommand
 * Then register it with registerCommand() in the static constructor
 * 
 * Try to keep names and aliases in lowercase
 * 
 * @author Schmoller
 *
 */
public class CommandDispatcher implements CommandExecutor
{
	static
	{
		mCommands = new HashMap<String, ICommand>();
		registerCommand(new SpyCommand());
		registerCommand(new StopSpyCommand());
		registerCommand(new PlaybackCommand());
		registerCommand(new ListCommand());
		registerCommand(new HelpCommand());
		registerCommand(new PurgeCommand());
		registerCommand(new InspectCommand());
		registerCommand(new HistoryCommand());
		registerCommand(new DebugCommand());
	}
	/**
	 * Registers a command to be handled by this dispatcher
	 * @param command
	 */
	private static void registerCommand(ICommand command)
	{
		mCommands.put(command.getName().toLowerCase(), command);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) 
	{
		if(args.length == 0)
		{
			displayUsage(sender, label, null);
			return true;
		}
		
		String subCommand = args[0].toLowerCase();
		String[] subArgs = (args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
		
		ICommand com = null;
		if(mCommands.containsKey(subCommand))
		{
			com = mCommands.get(subCommand);
		}
		else
		{
			// Check aliases
AliasCheck:	for(Entry<String, ICommand> ent : mCommands.entrySet())
			{
				if(ent.getValue().getAliases() != null)
				{
					String[] aliases = ent.getValue().getAliases();
					for(String alias : aliases)
					{
						if(subCommand.equalsIgnoreCase(alias))
						{
							com = ent.getValue();
							break AliasCheck;
						}
					}
				}
			}
		}
		
		// Was not found
		if(com == null)
		{
			displayUsage(sender,label, subCommand);
			return true;
		}
		
		// Check that the sender is correct
		if(!com.canBeConsole() && sender instanceof ConsoleCommandSender)
		{
			sender.sendMessage(ChatColor.RED + "Only players can call /" + label + " " + subCommand);
			return true;
		}
		
		// Check that they have permission
		if(com.getPermission() != null && !sender.hasPermission(com.getPermission()))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission to use /" + label + " " + subCommand);
		}
		
		if(!com.onCommand(sender, subCommand, subArgs))
		{
			sender.sendMessage(ChatColor.RED + "Usage: /" + label + " " + com.getUsageString(subCommand));
		}
		
		return true;
	}
	private void displayUsage(CommandSender sender, String label, String subcommand)
	{
		String usage = "";
		
		if(subcommand != null)
		{
			sender.sendMessage(ChatColor.RED + "Unknown command: " + ChatColor.RESET + "/" + label + " " + ChatColor.GOLD + subcommand);
			sender.sendMessage("Valid commands are:");
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "No command specified: " + ChatColor.RESET + "/" + label + ChatColor.GOLD + " <command>");
			sender.sendMessage("Valid commands are:");;
		}
		
		boolean first = true;
		// Build the list
		for(Entry<String, ICommand> ent : mCommands.entrySet())
		{
			if(first)
				usage += ent.getKey();
			else
				usage += ", " + ent.getKey();
			
			first = false;
		}
		
		sender.sendMessage(usage);
	}

	private static HashMap<String, ICommand> mCommands;
}
