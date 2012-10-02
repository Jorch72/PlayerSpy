package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.*;

public class ItemPickupRecord extends Record 
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
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		mItem.writeItemStack(stream);
		mLocation.writeLocation(stream, false);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mItem = StoredItemStack.readItemStack(stream);
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
	protected int getContentSize() 
	{
		return mItem.getSize() + mLocation.getSize(false);
	}
	
}
