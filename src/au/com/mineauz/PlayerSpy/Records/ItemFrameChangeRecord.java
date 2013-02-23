package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.ILocationAware;
import au.com.mineauz.PlayerSpy.Records.IRollbackable;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.storage.StoredItemFrame;

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
	@SuppressWarnings( "deprecation" )
	public ItemFrameChangeRecord(au.com.mineauz.PlayerSpy.legacy.v2.ItemFrameChangeRecord old)
	{
		super(RecordType.ItemFrameChange);
		
		mFrame = new StoredItemFrame(old.getItemFrame());
		mPlaced = old.getPlaced();
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeBoolean(mPlaced);
		mFrame.write(stream, absolute);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		mPlaced = stream.readBoolean();
		
		mFrame = StoredItemFrame.read(stream, currentWorld, absolute);
	}
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return mFrame.getSize(absolute) + 1; 
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
	public boolean rollback( boolean preview, Player previewTarget )
	{
		if(!preview)
		{
			ItemFrame frame = getLocation().getWorld().spawn(getLocation(), ItemFrame.class);
			frame.setFacingDirection(mFrame.getBlockFace());
			frame.setItem(mFrame.getItem());
		}
		return false;
	}
	@Override
	public boolean restore()
	{
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public Location getLocation()
	{
		return mFrame.getLocation();
	}
}
