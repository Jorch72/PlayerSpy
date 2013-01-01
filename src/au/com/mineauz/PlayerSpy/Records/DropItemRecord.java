package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.StoredItemStack;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

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
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
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

	@Override
	public String getDescription()
	{
		return mStack.getItem().getAmount() + "x " + ChatColor.DARK_AQUA + Utility.formatItemName(mStack.getItem()) + ChatColor.RESET + " dropped by %s";
	}
}
