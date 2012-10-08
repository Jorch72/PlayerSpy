package au.com.mineauz.PlayerSpy.legacy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.StoredItemStack;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;

@Deprecated
public class UpdateInventoryRecord extends Record 
{

	public UpdateInventoryRecord(int slot, ItemStack item) 
	{
		super(RecordType.UpdateInventory);
		mSlotId = slot;
		if(item != null)
			mItem = item.clone();
	}
	public UpdateInventoryRecord() 
	{
		super(RecordType.UpdateInventory);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeByte(mSlotId);
		new StoredItemStack(mItem).writeItemStack(stream);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mSlotId = stream.readByte();
		mItem = StoredItemStack.readItemStack(stream).getItem();
		if(mItem.getTypeId() == 0)
			mItem = null;
	}
	public static UpdateInventoryRecord read(DataInputStream stream) throws IOException 
	{
		UpdateInventoryRecord record = new UpdateInventoryRecord();
		record.mSlotId = stream.readByte();
		record.mItem = StoredItemStack.readItemStack(stream).getItem();
		if(record.mItem.getTypeId() == 0)
			record.mItem = null;
		
		return record;
	}
	
	public int getSlotId()
	{
		return mSlotId;
	}
	public ItemStack getItem()
	{
		return mItem;
	}
	private int mSlotId;
	private ItemStack mItem;
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 1 + new StoredItemStack(mItem).getSize();
	}
}
