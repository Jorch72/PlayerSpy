package au.com.mineauz.PlayerSpy.commands;

import java.util.List;

import net.minecraft.server.ItemStack;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.StringTranslator;
import au.com.mineauz.PlayerSpy.Utility;

public class DebugCommand implements ICommand
{

	@Override
	public String getName() 
	{
		return "debug";
	}

	@Override
	public String[] getAliases() 
	{
		return null;
	}

	@Override
	public String getPermission() 
	{
		return null;
	}

	@Override
	public String getUsageString(String label) 
	{
		return label;
	}

	@Override
	public boolean canBeConsole() 
	{
		return false;
	}

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		Player player = (Player)sender;
		ItemStack stack  = Utility.convertToNative(player.getItemInHand());
		sender.sendMessage(StringTranslator.translateName(stack.getItem().c(stack)));
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
