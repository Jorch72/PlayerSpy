package au.com.mineauz.PlayerSpy.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.*;
import au.com.mineauz.PlayerSpy.Utilities.CharType;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class FilterConstraint extends Constraint
{
	private Pattern mPattern;
	public FilterConstraint(String pattern)
	{
		pattern = pattern.toLowerCase();
		// Escape all symbols
		for(int i = 0; i < pattern.length(); ++i)
		{
			char ch = pattern.charAt(i);
			if(CharType.get(ch) == CharType.Symbol)
			{
				if(ch != '.')
				{
					pattern = pattern.substring(0,i) + "\\" + pattern.substring(i);
					++i;
				}
			}
		}
		
		pattern = pattern.replaceAll("\\.", ".+?");
		
		mPattern = Pattern.compile(pattern);
	}
	@Override
	public boolean matches( Record record )
	{
		String matchString = null;
		switch(record.getType())
		{
		case Attack:
			matchString = ((AttackRecord)record).getDamagee().getName();
			break;
		case BlockChange:
			matchString = Utility.formatItemName(new ItemStack(((BlockChangeRecord)record).getBlock().getTypeId(), 1, ((BlockChangeRecord)record).getBlock().getData()));
			break;
		case ChatCommand:
			matchString = ((ChatCommandRecord)record).getMessage();
			break;
		case Damage:
			if(((DamageRecord)record).getDamager() != null)
				matchString = ((DamageRecord)record).getDamager().getName();
			break;
		case Death:
			matchString = ((DeathRecord)record).getReason();
			break;
		case DropItem:
			matchString = Utility.formatItemName(((DropItemRecord)record).getItem());
			break;
		case GameMode:
			matchString = GameMode.values()[((GameModeRecord)record).getGameMode()].toString();
			break;
		case Interact:
			if(((InteractRecord)record).hasBlock())
				matchString = Utility.formatItemName(new ItemStack(((InteractRecord)record).getBlock().getTypeId(),1,((InteractRecord)record).getBlock().getData()));
			else if(((InteractRecord)record).hasEntity())
				matchString = ((InteractRecord)record).getEntity().getName();
			else if(((InteractRecord)record).hasItem())
				matchString = Utility.formatItemName(((InteractRecord)record).getItem());
			
			break;
		case ItemFrameChange:
			matchString = "itemframe";
			break;
		case ItemPickup:
			matchString = Utility.formatItemName(((ItemPickupRecord)record).getItemStack());
			break;
		case ItemTransaction:
			matchString = ((InventoryTransactionRecord)record).getDescription();
			break;
		case Logoff:
			matchString = ((LogoffRecord)record).getReason();
			break;
		case PaintingChange:
			matchString = "painting";
			break;
		case VehicleMount:
			matchString = ((VehicleMountRecord)record).getVehicle().getName();
			break;
		default:
			break;
		
		}
		
		if(matchString == null)
			return false;
		
		Matcher m = mPattern.matcher(matchString.toLowerCase());
		return m.find(); 
	}

	@Override
	public String getDescription()
	{
		return null;
	}

}
