package au.com.mineauz.PlayerSpy.legacy.v2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import au.com.mineauz.PlayerSpy.Records.ILocationAware;
import au.com.mineauz.PlayerSpy.Records.IRollbackable;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.storage.StoredBlock;

@Deprecated
public class BlockChangeRecord extends Record implements IRollbackable, ILocationAware
{
	public StoredBlock mInitialBlock;
	public StoredBlock mFinalBlock;
	public boolean mPlaced;
	private boolean mIsRolledBack;
	
	public BlockChangeRecord()
	{
		super(RecordType.BlockChange);
	}
	public BlockChangeRecord(BlockState initialBlock, BlockState finalBlock, boolean place)
	{
		super(RecordType.BlockChange);
		if(initialBlock == null && finalBlock != null)
			mInitialBlock = new StoredBlock(finalBlock.getLocation(),Material.AIR, (byte)0);
		else if(finalBlock == null && initialBlock != null)
			mFinalBlock = new StoredBlock(initialBlock.getLocation(), Material.AIR, (byte)0);
		
		if(initialBlock != null)
			mInitialBlock = new StoredBlock(initialBlock);
		if(finalBlock != null)
			mFinalBlock = new StoredBlock(finalBlock);

		mPlaced = place;
		mIsRolledBack = false;
	}
	public BlockChangeRecord(MaterialData initialBlock, MaterialData finalBlock, Location location, boolean place)
	{
		super(RecordType.BlockChange);
		if(initialBlock == null && finalBlock != null)
			mInitialBlock = new StoredBlock(location,Material.AIR, (byte)0);
		else if(finalBlock == null && initialBlock != null)
			mFinalBlock = new StoredBlock(location, Material.AIR, (byte)0);
		if(initialBlock != null)
			mInitialBlock = new StoredBlock(initialBlock, location);
		if(finalBlock != null)
			mFinalBlock = new StoredBlock(finalBlock, location);
		
		mPlaced = place;
		mIsRolledBack = false;
	}
	
	public BlockChangeRecord(au.com.mineauz.PlayerSpy.legacy.BlockChangeRecord old)
	{
		super(RecordType.BlockChange);
		
		setInitialBlock(new StoredBlock(old.getInitialBlock().BlockLocation, Material.getMaterial(old.getInitialBlock().BlockId), old.getInitialBlock().BlockData));
		setFinalBlock(new StoredBlock(old.getFinalBlock().BlockLocation, Material.getMaterial(old.getFinalBlock().BlockId), old.getFinalBlock().BlockData));
		setPlaced(old.getFinalBlock().BlockId != 0);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeBoolean(mPlaced);
		mInitialBlock.write(stream, absolute, false);
		mFinalBlock.write(stream, absolute, false);
		stream.writeBoolean(mIsRolledBack);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		mPlaced = stream.readBoolean();
		mInitialBlock = new StoredBlock();
		mInitialBlock.read(stream, currentWorld, absolute);
		
		mFinalBlock = new StoredBlock();
		mFinalBlock.read(stream, currentWorld, absolute);
		
		mIsRolledBack = stream.readBoolean();
	}

	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 2 + mInitialBlock.getSize(absolute, false) + mFinalBlock.getSize(absolute, false);
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
	@Override
	public String getDescription()
	{
		String blockName = Utility.formatItemName(new ItemStack(getBlock().getType(), 1, getBlock().getData()));
		
		return ChatColor.DARK_AQUA + blockName + ChatColor.RESET + " was " + (mPlaced ? "placed" : "removed") + " by %s";
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
	public boolean restore()
	{
		return false;
	}
	@Override
	public boolean rollback( boolean preview, Player previewTarget )
	{
		return false;
	}
	@Override
	public void setRollbackState( boolean state )
	{
		mIsRolledBack = state;
	}
}
