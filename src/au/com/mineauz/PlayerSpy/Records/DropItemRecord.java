package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.StoredItemStack;

public class DropItemRecord extends Record 
{

	public DropItemRecord(ItemStack stack) 
	{
		super(RecordType.DropItem);
		mStack = new StoredItemStack(stack);
	}
	public DropItemRecord()
	{
		super(RecordType.DropItem);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		mStack.writeItemStack(stream);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mStack = StoredItemStack.readItemStack(stream);
	}

	@Override
	protected int getContentSize(boolean absolute) 
	{
		return mStack.getSize();
	}

	public ItemStack getItem()
	{
		return mStack.getItem();
	}
	
	private StoredItemStack mStack;
}
