package au.com.mineauz.PlayerSpy.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.craftbukkit.v1_6_R2.inventory.CraftInventory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.inventory.Inventory;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.debugging.Debug;


@SuppressWarnings( "deprecation" )
public class StoredInventoryInformation 
{
	public enum InventoryType
	{
		Player,
		Enderchest,
		Chest,
		Entity,
		None,
		Unknown,
		Anvil,
		EnchantingTable,
		Crafting
	}
	
	private String mPlayerName;
	private StoredBlock mBlock;
	private StoredEntity mEntity;
	private InventoryType mType;
	
	public StoredInventoryInformation(Inventory inventory, Location enderChestLocation)
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
			
			if(enderChestLocation != null)
				mBlock = new StoredBlock(enderChestLocation.getBlock());
		}
		else if(inventory.getHolder() instanceof Player)
		{
			if(inventory.getType() == org.bukkit.event.inventory.InventoryType.MERCHANT)
			{
				mType = InventoryType.Entity;
				mEntity = new StoredEntity((Player)inventory.getHolder());
				mEntity.setEntityType(EntityType.VILLAGER);
			}
			else if(inventory.getType() == org.bukkit.event.inventory.InventoryType.PLAYER)
			{
				mType = InventoryType.Player;
				
				mPlayerName = ((Player)inventory.getHolder()).getName();
			}
			else if(inventory.getType() == org.bukkit.event.inventory.InventoryType.ANVIL)
			{
				mType = InventoryType.Anvil;
				if(enderChestLocation != null)
					mBlock = new StoredBlock(enderChestLocation.getBlock());
				mPlayerName = ((Player)inventory.getHolder()).getName();
			}
			else if(inventory.getType() == org.bukkit.event.inventory.InventoryType.ENCHANTING)
			{
				mType = InventoryType.EnchantingTable;
				if(enderChestLocation != null)
					mBlock = new StoredBlock(enderChestLocation.getBlock());
				mPlayerName = ((Player)inventory.getHolder()).getName();
			}
			else if(inventory.getType() == org.bukkit.event.inventory.InventoryType.CRAFTING || inventory.getType() == org.bukkit.event.inventory.InventoryType.WORKBENCH)
			{
				mType = InventoryType.Crafting;
				if(enderChestLocation != null)
					mBlock = new StoredBlock(enderChestLocation.getBlock());
				mPlayerName = ((Player)inventory.getHolder()).getName();
			}
			else
			{
				Debug.info("Unknown inventory type with holder player. Type: " + inventory.getType() + " holder:" + ((Player)inventory.getHolder()).getName() + " world:" + ((Player)inventory.getHolder()).getWorld().getName());
				mType = InventoryType.Unknown;
			}
			
			// In an alternate universe, this would have been how to deal with enderchests, but no, the bukkit devs fucked up again. -_-
			//else if(inventory.getType() == org.bukkit.event.inventory.InventoryType.ENDER_CHEST)
				//mType = InventoryType.Enderchest;
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
			mBlock.write(stream, absolute, false);
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
		case Anvil:
		case EnchantingTable:
		case Crafting:
		case Enderchest:
			stream.writeUTF(mPlayerName);
			if(mBlock == null)
				stream.writeBoolean(false);
			else
			{
				stream.writeBoolean(true);
				mBlock.write(stream, absolute, false);
			}
			break;
		}
	}
	
	public void read(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		int typeId = stream.readByte();
		if(typeId < 0 || typeId >= InventoryType.values().length)
			throw new RecordFormatException("Bad inventory type id " + typeId);
		
		mType = InventoryType.values()[typeId];
		
		try
		{
			switch(mType)
			{
			case Chest:
				mBlock = new StoredBlock();
				mBlock.read(stream, currentWorld, absolute);
				break;
			case Anvil:
			case EnchantingTable:
			case Crafting:
			case Enderchest:
				mPlayerName = stream.readUTF();
				if(stream.readBoolean())
				{
					mBlock = new StoredBlock();
					mBlock.read(stream, currentWorld, absolute);
				}
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
		catch(UTFDataFormatException e)
		{
			throw new RecordFormatException("Error reading UTF string. Malformed data.");
		}
	}
	
	public int getSize(boolean absolute)
	{
		int size = 1;
		
		switch(mType)
		{
		case Chest:
			size += mBlock.getSize(absolute, false);
			break;
		case Anvil:
		case EnchantingTable:
		case Crafting:
		case Enderchest:
			size += Utility.getUTFLength(mPlayerName) + 1;
			if(mBlock != null)
				size += mBlock.getSize(absolute, false);
			break;
		case Entity:
			size += mEntity.getSize();
			break;
		case Player:
			size += Utility.getUTFLength(mPlayerName);
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
		case Anvil:
		case EnchantingTable:
		case Crafting:
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
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof StoredInventoryInformation))
			return false;
		
		StoredInventoryInformation inv = (StoredInventoryInformation)obj;
		
		if(mType != inv.mType)
			return false;
		
		switch(mType)
		{
		case Chest:
			return mBlock.equals(inv.mBlock);
		case Enderchest:
		case Player:
			return mPlayerName.equals(inv.mPlayerName);
		case Entity:
			return mEntity.equals(inv.mEntity);
		default:
			return true;
		}
	}
}
