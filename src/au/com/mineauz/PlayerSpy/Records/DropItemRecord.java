package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.storage.StoredItemStack;


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
	
	@SuppressWarnings( "deprecation" )
	public DropItemRecord(au.com.mineauz.PlayerSpy.legacy.v2.DropItemRecord old)
	{
		super(RecordType.DropItem);
		mStack = new StoredItemStack(old.getItem());
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
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof DropItemRecord))
			return false;
		
		return mStack.equals(((DropItemRecord)obj).mStack);
	}
}
