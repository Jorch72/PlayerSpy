package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.Pager;
import au.com.mineauz.PlayerSpy.SpyPlugin;

public class ListCommand implements ICommand
{
	@Override
	public String getName() 
	{
		return "list";
	}

	@Override
	public String[] getAliases() 
	{
		return new String[] { "l" };
	}

	@Override
	public String getPermission() 
	{
		return "playerspy.list";
	}

	@Override
	public String getUsageString(String label) 
	{
		return label + " " + ChatColor.GREEN + "[page]";
	}

	@Override
	public boolean canBeConsole() { return true; }

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length > 1)
			return false;
		
		Pager pager = new Pager("All Recordings", (sender instanceof Player ? 8 : 15));
		
		List<String> monitors = SpyPlugin.getInstance().getMonitorTargets();
		List<String> playbacks = SpyPlugin.getInstance().getPlaybackTargets();
		
		for(String target : monitors)
		{
			if(playbacks.contains(target))
				pager.addItem(target + ChatColor.LIGHT_PURPLE + " [Timeshifting]");
			else
				pager.addItem(target + ChatColor.RED + " [Recording]");
		}
		
		for(String target : playbacks)
		{
			if(!monitors.contains(target))
				pager.addItem(target + ChatColor.BLUE + " [Playing]");
		}

		int page = 0;
		if(args.length == 2)
		{
			try
			{
				page = Integer.parseInt(args[1]);
			}
			catch(NumberFormatException e)
			{
				sender.sendMessage(ChatColor.RED + "Usage: /" + label + " list [page]");
				return true;
			}
		}
		
		pager.displayPage(sender, page);
		return true;
	}

}