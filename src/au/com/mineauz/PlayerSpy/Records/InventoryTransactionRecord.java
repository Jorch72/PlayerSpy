package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.StoredInventoryInformation;
import au.com.mineauz.PlayerSpy.StoredItemStack;

public class InventoryTransactionRecord extends Record
{
	private ItemStack mItem;
	private StoredInventoryInformation mInvInfo;
	private boolean mTake;
	
	public static InventoryTransactionRecord newTakeFromInventory(ItemStack item, Inventory inventory)
	{
		InventoryTransactionRecord record = new InventoryTransactionRecord();
		record.mItem = item.clone();
		record.mInvInfo = new StoredInventoryInformation(inventory);
		record.mTake = true;
		return record;
	}
	public static InventoryTransactionRecord newAddToInventory(ItemStack item, Inventory inventory)
	{
		InventoryTransactionRecord record = new InventoryTransactionRecord();
		record.mItem = item.clone();
		record.mInvInfo = new StoredInventoryInformation(inventory);
		record.mTake = false;
		return record;
	}

	public InventoryTransactionRecord() 
	{
		super(RecordType.ItemTransaction);
	}

	/**
	 * Gets the item involved
	 */
	public ItemStack getItem()
	{
		return mItem;
	}
	/**
	 * Gets information about the inventory
	 */
	public StoredInventoryInformation getInventoryInfo()
	{
		return mInvInfo;
	}
	
	/**
	 * Gets whether they are taking from the inventory
	 */
	public boolean isTaking()
	{
		return mTake;
	}
	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeBoolean(mTake);
		new StoredItemStack(mItem).writeItemStack(stream);
		mInvInfo.write(stream, absolute);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mTake = stream.readBoolean();
		mItem = StoredItemStack.readItemStack(stream).getItem();
		mInvInfo = new StoredInventoryInformation();
		mInvInfo.read(stream, currentWorld, absolute);
	}

	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 1 + new StoredItemStack(mItem).getSize() + mInvInfo.getSize(absolute);
	}

	@Override
	public String toString() 
	{
		return "InventoryTransaction {Item: " + mItem.toString() + ", Dir: " + (mTake ? "Taking" : "Putting") + ", " + mInvInfo.toString();
	}
}
