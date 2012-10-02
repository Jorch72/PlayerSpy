package au.com.mineauz.PlayerSpy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Art;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Painting;


public class StoredPainting 
{
	public StoredPainting(Painting painting)
	{
		mArt = painting.getArt().getId();
		mFace = painting.getFacing().ordinal();
		mLocation = new StoredLocation(painting.getLocation());
	}
	private StoredPainting()
	{
		
	}
	
	public Art getArt()
	{
		return Art.getById(mArt);
	}
	public BlockFace getBlockFace()
	{
		return BlockFace.values()[mFace];
	}
	public Location getLocation()
	{
		return mLocation.getLocation();
	}
	
	public boolean writePainting(DataOutputStream stream)
	{
		try
		{
			stream.writeInt(mArt);
			stream.writeInt(mFace);
			mLocation.writeLocation(stream, false);
			
			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
	
	public static StoredPainting readPainting(DataInputStream stream, World currentWorld)
	{
		try
		{
			StoredPainting painting = new StoredPainting();
			painting.mArt = stream.readInt();
			painting.mFace = stream.readInt();
			painting.mLocation = StoredLocation.readLocation(stream,currentWorld);
			
			return painting;
		}
		catch(IOException e)
		{
			return null;
		}
	}
	
	public int getSize()
	{
		return 8 + mLocation.getSize(false);
	}
	private int mArt;
	private int mFace;
	private StoredLocation mLocation;
}
