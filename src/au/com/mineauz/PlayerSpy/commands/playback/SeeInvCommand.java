package au.com.mineauz.PlayerSpy.commands.playback;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import au.com.mineauz.PlayerSpy.PlaybackContext;
import au.com.mineauz.PlayerSpy.commands.Command;

public class SeeInvCommand extends Command
{

	@Override
	public boolean onCommand(Player sender, PlaybackContext playback, String[] args) 
	{
		if(args.length == 0)
		{
			Inventory inv = playback.getTargetInventory(0);
			sender.openInventory(inv);
			return true;
		}
		return false;
	}

	@Override
	public String getUsage() 
	{
		return "seeinv";
	}

	@Override
	public String getDescription() 
	{
		return "Shows the playbacks current inventory status";
	}
	
}
