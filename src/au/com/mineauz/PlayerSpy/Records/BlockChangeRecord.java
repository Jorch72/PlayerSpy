package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

import au.com.mineauz.PlayerSpy.StoredBlock;

public class BlockChangeRecord extends Record
{
	public StoredBlock mInitialBlock;
	public StoredBlock mFinalBlock;
	public boolean mPlaced;
	
	public BlockChangeRecord()
	{
		super(RecordType.BlockChange);
	}
	public BlockChangeRecord(Block initialBlock, Block finalBlock, boolean place)
	{
		super(RecordType.BlockChange);
		if(initialBlock == null && finalBlock != null)
			mInitialBlock = new StoredBlock(finalBlock.getLocation(),Material.AIR, (byte)0);
		else if(finalBlock == null && initialBlock != null)
			mFinalBlock = new StoredBlock(initialBlock.getLocation(), Material.AIR, (byte)0);
		
		mInitialBlock = new StoredBlock(initialBlock);
		mFinalBlock = new StoredBlock(finalBlock);
		mPlaced = place;
	}
	public BlockChangeRecord(BlockState initialBlock, BlockState finalBlock, boolean place)
	{
		super(RecordType.BlockChange);
		if(initialBlock == null && finalBlock != null)
			mInitialBlock = new StoredBlock(finalBlock.getLocation(),Material.AIR, (byte)0);
		else if(finalBlock == null && initialBlock != null)
			mFinalBlock = new StoredBlock(initialBlock.getLocation(), Material.AIR, (byte)0);
		
		mInitialBlock = new StoredBlock(initialBlock);
		mFinalBlock = new StoredBlock(finalBlock);
		mPlaced = place;
	}
	public BlockChangeRecord(MaterialData initialBlock, MaterialData finalBlock, Location location, boolean place)
	{
		super(RecordType.BlockChange);
		if(initialBlock == null && finalBlock != null)
			mInitialBlock = new StoredBlock(location,Material.AIR, (byte)0);
		else if(finalBlock == null && initialBlock != null)
			mFinalBlock = new StoredBlock(location, Material.AIR, (byte)0);
		
		mInitialBlock = new StoredBlock(initialBlock, location);
		mFinalBlock = new StoredBlock(finalBlock, location);
		mPlaced = place;
	}

	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		stream.writeBoolean(mPlaced);
		mInitialBlock.write(stream);
		mFinalBlock.write(stream);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mPlaced = stream.readBoolean();
		mInitialBlock = new StoredBlock();
		mInitialBlock.read(stream, currentWorld);
		
		mFinalBlock = new StoredBlock();
		mFinalBlock.read(stream, currentWorld);
	}

	@Override
	protected int getContentSize() 
	{
		return 1 + mInitialBlock.getSize() + mFinalBlock.getSize();
	}

	public StoredBlock getInitialBlock()
	{
		return mInitialBlock;
	}
	public void setInitialBlock(StoredBlock block)
	{
		mInitialBlock = block;
	}
	public StoredBlock getFinalBlock()
	{
		return mFinalBlock;
	}
	public void setFinalBlock(StoredBlock block)
	{
		mFinalBlock = block;
	}
	public StoredBlock getBlock()
	{
		if(mPlaced)
			return mFinalBlock;
		else
			return mInitialBlock;
	}
	public Location getLocation()
	{
		if(mPlaced)
			return mFinalBlock.getLocation();
		else
			return mInitialBlock.getLocation();
	}
	public boolean wasPlaced()
	{
		return mPlaced;
	}
	public void setPlaced(boolean placed)
	{
		mPlaced = placed;
	}
}
