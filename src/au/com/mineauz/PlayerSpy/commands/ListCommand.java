package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Utilities.Pager;
import au.com.mineauz.PlayerSpy.monitoring.DeepMonitor;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;

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
		
		List<DeepMonitor> monitors = GlobalMonitor.instance.getAllDeepMonitors();
		List<String> playbacks = SpyPlugin.getInstance().getPlaybackTargets();
		
		for(DeepMonitor monitor : monitors)
		{
			if(playbacks.contains(monitor.getMonitorTarget().getName()))
				pager.addItem(monitor.getMonitorTarget().getName() + ChatColor.LIGHT_PURPLE + " [Timeshifting]");
			else
				pager.addItem(monitor.getMonitorTarget().getName() + ChatColor.RED + " [Recording]");
		}
		
		for(String target : playbacks)
		{
			boolean ok = true;
			for(DeepMonitor monitor : monitors)
			{
				if(monitor.getMonitorTarget().getName().equals(target))
				{
					ok = false;
					break;
				}
			}
			if(ok)
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

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
