package au.com.mineauz.PlayerSpy.search;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.DropItemRecord;
import au.com.mineauz.PlayerSpy.Records.ItemPickupRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class ItemConstraint extends Constraint
{
	public Pair<Material,Integer> material;
	public int amount;
	public boolean pickup;
	
	@Override
	public boolean matches( Record record )
	{
		if((pickup && !(record instanceof ItemPickupRecord)) || (!pickup && !(record instanceof DropItemRecord)))
			return false;
		
		ItemStack item;
		
		if(pickup)
			item = ((ItemPickupRecord)record).getItemStack();
		else
			item = ((DropItemRecord)record).getItem();

		boolean matOk = false;
		if(material.getArg1() == Material.AIR)
			matOk = true;
		else if(material.getArg1() == item.getType())
		{
			if(material.getArg2() == -1)
				matOk = true;
			else if(material.getArg2() == item.getDurability())
				matOk = true;
		}
		
		if(!matOk)
			return false;
		
		if(amount == 0 || item.getAmount() == amount)
			return true;
		
		return false;
	}

	@Override
	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
