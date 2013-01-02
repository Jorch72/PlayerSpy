package au.com.mineauz.PlayerSpy.search;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import au.com.mineauz.PlayerSpy.Records.InventoryTransactionRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;
import au.com.mineauz.PlayerSpy.storage.StoredInventoryInformation.InventoryType;

public class TransactionConstraint extends Constraint
{
	public Pair<Material,Integer> mFilterType;
	public int mAmount;
	public boolean mTook;
	
	public Object mTargetFilter;
	
	@Override
	public boolean matches( Record record )
	{
		if(!(record instanceof InventoryTransactionRecord))
			return false;
		
		InventoryTransactionRecord transaction = (InventoryTransactionRecord)record;
	
		if(mTook != transaction.isTaking())
			return false;
		
		boolean typeFilter = false;
		if(mFilterType.getArg1() == Material.AIR) // Any
			typeFilter = true;
		else if(transaction.getItem().getType() == mFilterType.getArg1())
		{
			if(mFilterType.getArg2() == -1) // Any
				typeFilter = true;
			else if(transaction.getItem().getDurability() == mFilterType.getArg2())
				typeFilter = true;
		}
		
		if(!typeFilter)
			return false;
		
		// Check the amount
		if(mAmount > 0 && transaction.getItem().getAmount() != mAmount)
			return false;
		
		// Check the target
		if(mTargetFilter == null)
			return true;
		
		if(mTargetFilter instanceof OfflinePlayer)
		{
			if(transaction.getInventoryInfo().getType() != InventoryType.Player && transaction.getInventoryInfo().getType() != InventoryType.Enderchest)
				return false;
			
			if(transaction.getInventoryInfo().getPlayerName().equalsIgnoreCase(((OfflinePlayer)mTargetFilter).getName()))
				return true;
		}
		else if(mTargetFilter instanceof Short)
		{
			if(transaction.getInventoryInfo().getType() != InventoryType.Entity)
				return false;
			
			if(transaction.getInventoryInfo().getEntity().getEntityType().getTypeId() == ((Short)mTargetFilter))
				return true;
		}
		else if(mTargetFilter instanceof Pair<?,?>)
		{
			@SuppressWarnings( "unchecked" )
			Pair<Material,Integer> blockFilter = (Pair<Material,Integer>)mTargetFilter;
			
			if(transaction.getInventoryInfo().getBlock() == null)
				return false;
			
			typeFilter = false;
			if(blockFilter.getArg1() == Material.AIR) // Any
				typeFilter = true;
			else if(transaction.getInventoryInfo().getBlock().getType() == blockFilter.getArg1())
			{
				if(blockFilter.getArg2() == -1) // Any
					typeFilter = true;
				else if(transaction.getInventoryInfo().getBlock().getData() == blockFilter.getArg2())
					typeFilter = true;
			}
			
			if(typeFilter)
				return true;
		}
		
		return false;
	}

}
