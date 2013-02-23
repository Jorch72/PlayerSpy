package au.com.mineauz.PlayerSpy.legacy.v2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.ILocationAware;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.legacy.StoredItemStack;
import au.com.mineauz.PlayerSpy.storage.StoredLocation;
@Deprecated
public class ItemPickupRecord extends Record implements ILocationAware
{

	public ItemPickupRecord(Item item) 
	{
		super(RecordType.ItemPickup);
		
		mLocation = new StoredLocation(item.getLocation());
		mItem = new StoredItemStack(item.getItemStack());
	}
	public ItemPickupRecord() 
	{
		super(RecordType.ItemPickup);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		mItem.writeItemStack(stream);
		mLocation.writeLocation(stream, absolute);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		mItem = StoredItemStack.readItemStack(stream);
		if(absolute)
			mLocation = StoredLocation.readLocationFull(stream);
		else
			mLocation = StoredLocation.readLocation(stream, currentWorld);
	}

	public Location getLocation()
	{
		return mLocation.getLocation();
	}
	public ItemStack getItemStack()
	{
		return mItem.getItem();
	}
	StoredLocation mLocation;
	StoredItemStack mItem;
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return mItem.getSize() + mLocation.getSize(absolute);
	}
	@Override
	public String getDescription()
	{
		String itemName = Utility.formatItemName(mItem.getItem());
		
		return mItem.getItem().getAmount() + "x " + ChatColor.DARK_AQUA + itemName + ChatColor.RESET + " picked up by %s";
	}
	
}
