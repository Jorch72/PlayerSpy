package au.com.mineauz.PlayerSpy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.inventory.Inventory;


public class StoredInventoryInformation 
{
	public enum InventoryType
	{
		Player,
		Enderchest,
		Chest,
		Entity,
		None,
		Unknown
	}
	
	private String mPlayerName;
	private StoredBlock mBlock;
	private StoredEntity mEntity;
	private InventoryType mType;
	
	public StoredInventoryInformation(Inventory inventory)
	{
		if(inventory == null)
		{
			mType = InventoryType.None;
		}
		else if(inventory.getType() == org.bukkit.event.inventory.InventoryType.ENDER_CHEST)
		{
			mType = InventoryType.Enderchest;
			// Yes thats right, the only way to find whose enderchest this is, we have to iterate through all online players
			for(Player player : Bukkit.getOnlinePlayers())
			{
				// AND compare the native type -_-
				if(((CraftInventory)player.getEnderChest()).getInventory() == ((CraftInventory)inventory).getInventory())
				{
					mPlayerName = player.getName();
					break;
				}
			}
		}
		else if(inventory.getHolder() instanceof Player)
		{
			// In an alternate universe, this would have been how to deal with enderchests, but no, the bukkit devs fucked up again. -_-
			//if(inventory.getType() == org.bukkit.event.inventory.InventoryType.ENDER_CHEST)
				//mType = InventoryType.Enderchest;
			//else
				mType = InventoryType.Player;
			
			mPlayerName = ((Player)inventory.getHolder()).getName();
		}
		else if(inventory.getHolder() instanceof HumanEntity)
		{
			mType = InventoryType.Entity;
			mEntity = new StoredEntity((HumanEntity)inventory.getHolder());
		}
		else if(inventory.getHolder() instanceof StorageMinecart)
		{
			mType = InventoryType.Entity;
			mEntity = new StoredEntity((Entity)inventory.getHolder());
		}
		else if(inventory.getHolder() instanceof DoubleChest)
		{
			mType = InventoryType.Chest;
			DoubleChest dchest = (DoubleChest)inventory.getHolder();
			Chest leftChest = (Chest)dchest.getLeftSide();
			
			mBlock = new StoredBlock(leftChest.getBlock());
		}
		else if(inventory.getHolder() instanceof BlockState)
		{
			mType = InventoryType.Chest;
			mBlock = new StoredBlock(((BlockState)inventory.getHolder()).getBlock());
		}
		else 
		{
			mType = InventoryType.Unknown;
		}
	}
	
	public StoredInventoryInformation()
	{
		mType = InventoryType.Unknown;
	}
	
	public InventoryType getType()
	{
		return mType;
	}
	public StoredBlock getBlock()
	{
		return mBlock;
	}
	public StoredEntity getEntity()
	{
		return mEntity;
	}
	public String getPlayerName()
	{
		return mPlayerName;
	}
	public void write(DataOutputStream stream, boolean absolute) throws IOException
	{
		stream.writeByte(mType.ordinal());
		
		switch(mType)
		{
		case Chest:
			mBlock.write(stream, absolute);
			break;
		case Enderchest:
			stream.writeUTF(mPlayerName);
			break;
		case Entity:
			mEntity.write(stream);
			break;
		case Player:
			stream.writeUTF(mPlayerName);
			break;
		case None:
		case Unknown:
			break;
		}
	}
	
	public void read(DataInputStream stream, World currentWorld, boolean absolute) throws IOException
	{
		mType = InventoryType.values()[stream.readByte()];
		
		switch(mType)
		{
		case Chest:
			mBlock = new StoredBlock();
			mBlock.read(stream, currentWorld, absolute);
			break;
		case Enderchest:
			mPlayerName = stream.readUTF();
			break;
		case Entity:
			mEntity = StoredEntity.readEntity(stream);
			break;
		case Player:
			mPlayerName = stream.readUTF();
			break;
		case None:
		case Unknown:
			break;
		}
	}
	
	public int getSize(boolean absolute)
	{
		int size = 1;
		
		switch(mType)
		{
		case Chest:
			size += mBlock.getSize(absolute);
			break;
		case Enderchest:
			size += 2 + mPlayerName.length();
			break;
		case Entity:
			size += mEntity.getSize();
			break;
		case Player:
			size += 2 + mPlayerName.length();
			break;
		case None:
		case Unknown:
			break;
		}
		
		return size;
	}
	
	public String toString()
	{
		String result = "Inventory {Type: " + mType.toString() + ", Owner: ";
		switch(mType)
		{
		case Chest:
			result += mBlock.getType().toString() + " at:" + mBlock.getLocation().toString();
			break;
		case Enderchest:
		case Player:
			result += mPlayerName;
			break;
		case Entity:
			result += mEntity.getEntityType().getName() + " at:" + mEntity.getLocation().toString();
			break;
		case None:
			result += "No Owner";
			break;
		case Unknown:
			result += "Unknown";
			break;
		}
		
		return result + " }";
	}
}
