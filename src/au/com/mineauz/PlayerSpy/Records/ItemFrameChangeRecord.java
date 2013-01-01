package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.StoredItemFrame;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class ItemFrameChangeRecord extends Record implements IRollbackable, ILocationAware
{
	private StoredItemFrame mFrame;
	
	private boolean mPlaced;
	private boolean mIsRolledBack;
	
	public ItemFrameChangeRecord(ItemFrame iframe, boolean place) 
	{
		super(RecordType.ItemFrameChange);
		mFrame = new StoredItemFrame(iframe);
		mPlaced = place;
		mIsRolledBack = false;
	}
	public ItemFrameChangeRecord()
	{
		super(RecordType.ItemFrameChange);
		mIsRolledBack = false;
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeBoolean(mPlaced);
		mFrame.write(stream, absolute);
		stream.writeBoolean(mIsRolledBack);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		mPlaced = stream.readBoolean();
		
		mFrame = StoredItemFrame.read(stream, currentWorld, absolute);
		mIsRolledBack = stream.readBoolean();
	}
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return mFrame.getSize(absolute) + 2; 
	}

	public StoredItemFrame getItemFrame()
	{
		return mFrame;
	}
	public boolean getPlaced()
	{
		return mPlaced;
	}
	
	
	
	@Override
	public String getDescription()
	{
		String name = ChatColor.DARK_AQUA + Utility.formatItemName(new ItemStack(Material.ITEM_FRAME)) + ChatColor.RESET;
		if(mPlaced)
			return "%s placed a " + name;
		else
			return "%s removed a " + name;
	}
	@Override
	public boolean canBeRolledBack()
	{
		return true;
	}
	@Override
	public boolean wasRolledBack()
	{
		return mIsRolledBack;
	}
	@Override
	public void setRolledBack( boolean value )
	{
		mIsRolledBack = value;
	}
	@Override
	public Location getLocation()
	{
		return mFrame.getLocation();
	}
}
