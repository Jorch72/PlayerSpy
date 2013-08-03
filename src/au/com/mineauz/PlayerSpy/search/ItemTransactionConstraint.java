package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Records.InventoryTransactionRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;

public class ItemTransactionConstraint extends RecordTypeConstraint
{
	private boolean mTake;
	private boolean mAdd;
	public ItemTransactionConstraint(boolean take, boolean add)
	{
		super(RecordType.ItemTransaction);
		
		mTake = take;
		mAdd = add;
	}
	
	@Override
	public boolean matches( Record record )
	{
		if(!super.matches(record))
			return false;
		
		return ((mTake && ((InventoryTransactionRecord)record).isTaking()) && (mAdd && !((InventoryTransactionRecord)record).isTaking()));
	}
	
	@Override
	public String getDescription()
	{
		if(mTake && mAdd)
			return "Transactions";
		else if(mTake)
			return "Removing Transactions";
		else if(mAdd)
			return "Adding Transactions";
		
		return null;
	}
}
