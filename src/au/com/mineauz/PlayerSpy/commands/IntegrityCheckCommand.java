package au.com.mineauz.PlayerSpy.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.LogTasks.IntegrityCheckTask;
import au.com.mineauz.PlayerSpy.LogTasks.IntegrityStats;
import au.com.mineauz.PlayerSpy.Utilities.Callback;

public class IntegrityCheckCommand implements ICommand
{
	private static WeakHashMap<CommandSender, IntegrityStats> mLastResults = new WeakHashMap<CommandSender, IntegrityStats>();
	
	public IntegrityCheckCommand()
	{
		
	}

	@Override
	public String getName()
	{
		return "integritycheck";
	}

	@Override
	public String[] getAliases()
	{
		return new String[] {"integrity"};
	}

	@Override
	public String getPermission()
	{
		return "playerspy.integritycheck";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		return new String[] {label + ChatColor.GREEN + " [discard]|[last]"};
	}

	@Override
	public String getDescription()
	{
		return "Performs a check of all every logs integrity. It will check for corrupted sessions. Optionally it can attempt to discard these sessions.";
	}

	@Override
	public boolean canBeConsole()
	{
		return true;
	}

	@Override
	public boolean canBeCommandBlock()
	{
		return false;
	}

	private void displayResults(CommandSender sender, IntegrityStats data, String label)
	{
		List<String> results = new ArrayList<String>();
		results.add("PlayerSpy Integrity Results:");
		results.add("");
		
		results.add(String.format("Log Load Stats: %d/%d", data.getLogCount() - data.getCorruptLogCount(), data.getLogCount()));
		results.add(String.format("Session Load Stats: %d/%d", data.getSessionCount() - data.getCorruptSessionCount(), data.getSessionCount()));
		
		results.add("");
		
		int integrity = (int)((((data.getLogCount() - data.getCorruptLogCount()) + (data.getSessionCount() - data.getCorruptSessionCount())) / (double)(data.getLogCount() + data.getSessionCount())) * 100);
		
		results.add(String.format("Integrity: %s%d%%", integrity > 90 ? ChatColor.GREEN : integrity > 50 ? ChatColor.YELLOW : ChatColor.RED, integrity));
		
		results.add("");
		
		results.add("Details:");
		
		for(String file : data.getOkLogs())
		{
			results.add(file);
			results.add("  " + (data.getSessionCount(file) - data.getCorruptSessionCount(file)) + "/" + data.getSessionCount(file) + " " + (int)(((data.getSessionCount(file) - data.getCorruptSessionCount(file)) / (double)data.getSessionCount(file)) * 100) + "%");
		}
		
		for(String file : data.getCorruptedLogs())
		{
			results.add(ChatColor.RED + file);
			results.add("  Load failed");
		}

		if(sender instanceof Player)
		{
			Player player = (Player)sender;
			if(player.isOnline())
			{
				sender.sendMessage(ChatColor.GOLD + "[PlayerSpy] " + ChatColor.WHITE + " Integrity check complete");
				ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
				
				BookMeta book = (BookMeta)item.getItemMeta();
				book.setTitle("Integrity Check");
				book.setAuthor("PlayerSpy");
				
				String page = "";
				int count = 0;
				for(String line : results)
				{
					if(!page.isEmpty())
						page += "\n";
					
					page += line;
					count++;
					
					if(count >= 13)
					{
						book.addPage(page);
						page = "";
						count = 0;
					}
				}
				
				if(count > 0)
				{
					book.addPage(page);
					page = "";
					count = 0;
				}
				
				item.setItemMeta(book);
				
				Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
				
				if(!remaining.isEmpty())
					sender.sendMessage(ChatColor.RED + "Your inventory is full, the book cannot be added. Make some room and call '/playerspy " + label + " last' to get the results again.");
				else
					sender.sendMessage("The book has been added. Use '/playerspy " + label + " last' to get the results again.");
			}
		}
		else
		{
			sender.sendMessage(results.toArray(new String[results.size()]));
			
			sender.sendMessage("Use '/playerspy " + label + " last' to get the results again.");
		}
	}
	@Override
	public boolean onCommand( final CommandSender sender, final String label, String[] args )
	{
		boolean discard = false;
		boolean last = false;
		
		if(args.length > 0)
		{
			if(args[0].equalsIgnoreCase("true"))
				discard = true;
			else if(args[0].equalsIgnoreCase("discard"))
				discard = true;
			else if(args[0].equalsIgnoreCase("last"))
				last = true;
			else if(!args[0].equalsIgnoreCase("false") && !args[0].equalsIgnoreCase("keep"))
			{
				sender.sendMessage(ChatColor.RED + "Expected true, false, discard, keep, last or nothing for first argument.");
				return true;
			}
		}
		
		if(last)
		{
			if(mLastResults.containsKey(sender))
			{
				displayResults(sender, mLastResults.get(sender), label);
				return true;
			}
			else
			{
				sender.sendMessage(ChatColor.RED + "You have not completed an integrity check yet.");
				return true;
			}
		}
		
		sender.sendMessage("The integrity is now being checked. This may take several minutes to complete");
		sender.sendMessage("depending on the number of logs to check. During this time you will be able");
		sender.sendMessage("to operate like normal. The process will not interfere in any activities");
		sender.sendMessage("including other playerspy activity.");
		
		sender.sendMessage("");
		if(sender instanceof Player)
			sender.sendMessage("You will receive a book with the results and notified when the check is complete");
		else
			sender.sendMessage("A printout of the results will be displayed when the check is complete");
		
		SpyPlugin.getExecutor().submit(new IntegrityCheckTask(discard), new Callback<IntegrityStats>()
		{
			@Override
			public void onSuccess( IntegrityStats data )
			{
				mLastResults.put(sender, data);
				displayResults(sender, data, label);
				if(sender instanceof Player)
					displayResults(Bukkit.getConsoleSender(), data, label);
			}

			@Override
			public void onFailure( Throwable error )
			{
				sender.sendMessage(ChatColor.GOLD + "[PlayerSpy] " + ChatColor.RED + "Failed to check integrity. Check the console for error.");
			}
		});
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		
		return null;
	}

}
