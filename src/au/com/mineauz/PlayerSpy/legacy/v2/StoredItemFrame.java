package au.com.mineauz.PlayerSpy.legacy.v2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.legacy.StoredItemStack;
import au.com.mineauz.PlayerSpy.storage.StoredLocation;

@Deprecated
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
	
	public void write(DataOutputStream stream, boolean absolute) throws IOException
	{
		stream.writeByte(mRotation.ordinal());
		stream.writeByte(mFacing.ordinal());
		mLocation.writeLocation(stream, absolute);
		new StoredItemStack(mItem).writeItemStack(stream);
	}
	
	public static StoredItemFrame read(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		StoredItemFrame frame = new StoredItemFrame();
		int rot = stream.readByte();
		if(rot < 0 || rot >= Rotation.values().length)
			throw new RecordFormatException("Bad rotation value " + rot);
		
		frame.mRotation = Rotation.values()[rot];
		
		int facing = stream.readByte();
		if(facing < 0 || facing >= BlockFace.values().length)
			throw new RecordFormatException("Bad facing value " + facing);
		
		frame.mFacing = BlockFace.values()[facing];
		
		if(absolute)
			frame.mLocation = StoredLocation.readLocationFull(stream);
		else
			frame.mLocation = StoredLocation.readLocation(stream,currentWorld);
		
		frame.mItem = StoredItemStack.readItemStack(stream).getItem();
		
		return frame;
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
