package au.com.mineauz.PlayerSpy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;


public class StoredItemFrame 
{
	public StoredItemFrame(ItemFrame frame)
	{
		mRotation = frame.getRotation();
		mItem = (frame.getItem() == null ? null : frame.getItem().clone());
		mFacing = frame.getFacing();
		mLocation = new StoredLocation(frame.getLocation());
	}
	private StoredItemFrame()
	{
		
	}
	
	public BlockFace getBlockFace()
	{
		return mFacing;
	}
	public Location getLocation()
	{
		return mLocation.getLocation();
	}
	public ItemStack getItem()
	{
		return mItem;
	}
	
	public Rotation getRotation()
	{
		return mRotation;
	}
	
	public boolean write(DataOutputStream stream, boolean absolute)
	{
		try
		{
			stream.writeByte(mRotation.ordinal());
			stream.writeByte(mFacing.ordinal());
			mLocation.writeLocation(stream, absolute);
			new StoredItemStack(mItem).writeItemStack(stream);
			
			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
	
	public static StoredItemFrame read(DataInputStream stream, World currentWorld, boolean absolute)
	{
		try
		{
			StoredItemFrame frame = new StoredItemFrame();
			frame.mRotation = Rotation.values()[stream.readByte()];
			frame.mFacing = BlockFace.values()[stream.readByte()];
			
			if(absolute)
				frame.mLocation = StoredLocation.readLocationFull(stream);
			else
				frame.mLocation = StoredLocation.readLocation(stream,currentWorld);
			
			frame.mItem = StoredItemStack.readItemStack(stream).getItem();
			
			return frame;
		}
		catch(IOException e)
		{
			return null;
		}
	}
	
	public int getSize(boolean absolute)
	{
		return 2 + mLocation.getSize(absolute) + new StoredItemStack(mItem).getSize();
	}
	private ItemStack mItem;
	private Rotation mRotation;
	private BlockFace mFacing;
	private StoredLocation mLocation;
}
