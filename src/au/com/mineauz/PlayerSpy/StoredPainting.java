package au.com.mineauz.PlayerSpy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Art;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Painting;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;


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
	
	public boolean writePainting(DataOutputStream stream, boolean absolute)
	{
		try
		{
			stream.writeInt(mArt);
			stream.writeInt(mFace);
			mLocation.writeLocation(stream, absolute);
			
			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
	
	public static StoredPainting readPainting(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		StoredPainting painting = new StoredPainting();
		painting.mArt = stream.readInt();
		painting.mFace = stream.readInt();
		if(absolute)
			painting.mLocation = StoredLocation.readLocationFull(stream);
		else
			painting.mLocation = StoredLocation.readLocation(stream,currentWorld);
		
		return painting;
	}
	
	public int getSize(boolean absolute)
	{
		return 8 + mLocation.getSize(absolute);
	}
	private int mArt;
	private int mFace;
	private StoredLocation mLocation;
}
