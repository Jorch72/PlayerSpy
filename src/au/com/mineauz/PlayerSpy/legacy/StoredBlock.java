package au.com.mineauz.PlayerSpy.legacy;

import java.io.*;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@Deprecated
public class StoredBlock
{
	public StoredBlock(Block block)
	{
		BlockId = block.getTypeId();
		BlockData = block.getData();
		BlockLocation = block.getLocation().clone();
	}
	public StoredBlock()
	{
		
	}
	public int BlockId;
	public byte BlockData;
	
	public Location BlockLocation;
	
	public boolean writeBlock(DataOutputStream stream)
	{
		try
		{
			stream.writeInt(BlockId);
			stream.writeByte(BlockData);
			// Block location
			stream.writeInt(BlockLocation.getBlockX());
			stream.writeInt(BlockLocation.getBlockY());
			stream.writeInt(BlockLocation.getBlockZ());
			
			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
	
	public static StoredBlock readBlock(DataInputStream stream, World currentWorld)
	{
		try
		{
			StoredBlock block = new StoredBlock();
			
			block.BlockId = stream.readInt();
			block.BlockData = stream.readByte();

			int x,y,z;
			// Block location
			x = stream.readInt();
			y = stream.readInt();
			z = stream.readInt();
			
			block.BlockLocation = new Location(currentWorld,x,y,z);
			
			return block;
		}
		catch(IOException e)
		{
			return null;
		}
	}
	
	public int getSize()
	{
		return 17;
	}
}
