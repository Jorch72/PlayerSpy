package au.com.mineauz.PlayerSpy.legacy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.block.Block;

import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;

@Deprecated
public class BlockChangeRecord extends Record
{

	public BlockChangeRecord(Block initialBlock, Block finalBlock) 
	{
		super(RecordType.BlockChange);
		mInitialBlock = new StoredBlock(initialBlock);
		mFinalBlock = new StoredBlock(finalBlock);
	}
	public BlockChangeRecord(StoredBlock initialBlock, StoredBlock finalBlock) 
	{
		super(RecordType.BlockChange);
		mInitialBlock = initialBlock;
		mFinalBlock = finalBlock;
	}
	public BlockChangeRecord()
	{
		super(RecordType.BlockChange);
	}
	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		mInitialBlock.writeBlock(stream);
		mFinalBlock.writeBlock(stream);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mInitialBlock = StoredBlock.readBlock(stream, currentWorld);
		mFinalBlock = StoredBlock.readBlock(stream, currentWorld);
	}

	public StoredBlock getInitialBlock()
	{
		return mInitialBlock;
	}

	public StoredBlock getFinalBlock()
	{
		return mFinalBlock;
	}
	
	private StoredBlock mInitialBlock;
	private StoredBlock mFinalBlock;
	@Override
	protected int getContentSize() 
	{
		return mInitialBlock.getSize() + mFinalBlock.getSize();
	}
}
