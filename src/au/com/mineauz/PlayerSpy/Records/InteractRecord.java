package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.StoredBlock;
import au.com.mineauz.PlayerSpy.StoredEntity;
import au.com.mineauz.PlayerSpy.StoredItemStack;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class InteractRecord extends Record implements ILocationAware
{
	public InteractRecord(Action type, Block block, ItemStack item, Entity ent) 
	{
		super(RecordType.Interact);
		mType = type;
		
		if(block != null)
			mBlock = new StoredBlock(block);
		if(item != null)
			mItem = new StoredItemStack(item);
		if(ent != null)
			mEntity = new StoredEntity(ent);
	}
	public InteractRecord(Action type, StoredBlock block, ItemStack item, StoredEntity ent) 
	{
		super(RecordType.Interact);
		mType = type;
		
		if(block != null)
			mBlock = block;
		if(item != null)
			mItem = new StoredItemStack(item);
		if(ent != null)
			mEntity = ent;
	}

	public InteractRecord() 
	{
		super(RecordType.Interact);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeByte(mType.ordinal());
		
		stream.writeBoolean(hasBlock());
		if(hasBlock())
			mBlock.write(stream, absolute);
		
		stream.writeBoolean(hasItem());
		if(hasItem())
			mItem.writeItemStack(stream);
		
		stream.writeBoolean(hasEntity());
		if(hasEntity())
			mEntity.write(stream);
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mType = Action.values()[stream.readByte()];
		
		// Block
		if(stream.readBoolean())
		{
			mBlock = new StoredBlock();
			mBlock.read(stream, currentWorld, absolute);
		}
		
		// Item
		if(stream.readBoolean())
			mItem = StoredItemStack.readItemStack(stream);
		
		// Entity
		if(stream.readBoolean())
			mEntity = StoredEntity.readEntity(stream);
	}

	public Action getAction()
	{
		return mType;
	}
	public boolean hasEntity()
	{
		return mEntity != null;
	}
	public StoredEntity getEntity()
	{
		return mEntity;
	}
	
	public boolean hasBlock()
	{
		return mBlock != null;
	}
	public StoredBlock getBlock()
	{
		return mBlock;
	}
	
	public boolean hasItem()
	{
		return mItem != null;
	}
	public ItemStack getItem()
	{
		if(mItem == null)
			return null;
		return mItem.getItem();
	}
	
	private Action mType;
	private StoredEntity mEntity;
	private StoredBlock mBlock;
	private StoredItemStack mItem;
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		int size = 4;
		
		if(hasBlock())
			size += mBlock.getSize(absolute);
		
		if(hasItem())
			size += mItem.getSize();
		
		if(hasEntity())
			size += mEntity.getSize();
		
		return size;
	}
	@Override
	public String getDescription()
	{
		if(hasBlock())
		{
			String blockName = Utility.formatItemName(new ItemStack(mBlock.getType(),1, mBlock.getData()));
			return ChatColor.DARK_AQUA + blockName + ChatColor.RESET + " used by %s";
		}
		else if(hasEntity())
		{
			String entityName = (mEntity.getEntityType() == EntityType.PLAYER ? mEntity.getPlayerName() : mEntity.getEntityType().getName());
			return "%s interacted with " + ChatColor.DARK_AQUA + entityName + ChatColor.RESET;
		}
		return null;
	}
	@Override
	public Location getLocation()
	{
		if(mBlock != null)
			return mBlock.getLocation();
		if(mEntity != null)
			return mEntity.getLocation();
		return null;
	}
}
