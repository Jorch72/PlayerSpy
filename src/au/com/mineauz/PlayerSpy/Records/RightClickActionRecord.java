package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.StoredEntity;
import au.com.mineauz.PlayerSpy.StoredItemStack;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class RightClickActionRecord extends Record 
{
	public enum Action
	{
		Shears,
		Eat,
		ToggleSit,
		PotionDrink,
		ProjectileFire,
		BowDrawback,
		FishCast,
		FishPullback
	}
	public RightClickActionRecord(Action action, ItemStack item, Entity entity) 
	{
		super(RecordType.RClickAction);
		
		mAction = action;
		mItem = new StoredItemStack(item);
		if(entity != null)
			mEntity = new StoredEntity(entity);
	}
	public RightClickActionRecord() 
	{
		super(RecordType.RClickAction);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeByte((byte)mAction.ordinal());
		mItem.writeItemStack(stream);
		stream.writeBoolean(mEntity != null);
		if(mEntity != null)
		{
			mEntity.write(stream);
		}
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mAction = Action.values()[stream.readByte()];
		mItem = StoredItemStack.readItemStack(stream);
		
		if(stream.readBoolean())
			mEntity = StoredEntity.readEntity(stream);
	}

	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 2 + mItem.getSize() + (mEntity != null ? mEntity.getSize() : 0);
	}
	
	public Action getAction()
	{
		return mAction;
	}
	public ItemStack getItem()
	{
		return mItem.getItem();
	}
	public StoredEntity getEntity()
	{
		return mEntity;
	}

	private Action mAction;
	private StoredItemStack mItem;
	private StoredEntity mEntity;
	@Override
	public String getDescription()
	{
		String entityName = (mEntity == null ? "" : (mEntity.getEntityType() == EntityType.PLAYER ? mEntity.getPlayerName() : mEntity.getEntityType().getName()));
		String itemName = (mItem == null ? "" : Utility.formatItemName(mItem.getItem()));
		
		switch(mAction)
		{
		case BowDrawback:
			break;
		case Eat:
			return "%s ate " + ChatColor.DARK_AQUA + itemName + ChatColor.RESET;
		case FishCast:
			break;
		case FishPullback:
			break;
		case PotionDrink:
			return "%s drank " + ChatColor.DARK_AQUA + itemName + ChatColor.RESET;
		case ProjectileFire:
			break;
		case Shears:
			return ChatColor.DARK_AQUA + entityName + ChatColor.RESET + " sheared by %s";
		case ToggleSit:
			return "%s toggled the sitting state of " + ChatColor.DARK_AQUA + entityName + ChatColor.RESET;
		default:
			break;
		}
		return null;
	}
}
