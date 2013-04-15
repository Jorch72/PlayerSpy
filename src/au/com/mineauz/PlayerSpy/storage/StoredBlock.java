package au.com.mineauz.PlayerSpy.storage;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Jukebox;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.wrappers.nbt.*;

public class StoredBlock 
{
	private Location mLocation;
	private Material mType;
	private byte mData;
	private NBTTagCompound mStateData;
	
	private NBTTagCompound stateToTagCompound(BlockState state)
	{
		NBTTagCompound root = new NBTTagCompound("");
		if(state instanceof Sign)
		{
			root.set("type", new NBTTagString("","sign"));
			
			NBTTagList lines = new NBTTagList("");
			lines.add(new NBTTagString("",((Sign)state).getLine(0)));
			lines.add(new NBTTagString("",((Sign)state).getLine(1)));
			lines.add(new NBTTagString("",((Sign)state).getLine(2)));
			lines.add(new NBTTagString("",((Sign)state).getLine(3)));
			
			root.set("lines", lines);
		}
		else if(state instanceof CreatureSpawner)
		{
			root.set("type", new NBTTagString("","spawner"));
			
			root.set("spawnType", new NBTTagString("", ((CreatureSpawner)state).getCreatureTypeName()));
			root.set("delay", new NBTTagInt("", ((CreatureSpawner)state).getDelay()));
		}
		else if(state instanceof Jukebox)
		{
			root.set("type", new NBTTagString("","jukebox"));
			
			root.set("disc", new NBTTagInt("", ((Jukebox)state).getPlaying().getId()));
		}
		else if(state instanceof NoteBlock)
		{
			root.set("type", new NBTTagString("","noteblock"));
			
			root.set("note", new NBTTagByte("", ((NoteBlock)state).getRawNote()));
		}
		else if(state instanceof Skull)
		{
			root.set("type", new NBTTagString("","skull"));
			
			root.set("skullType", new NBTTagInt("",((Skull)state).getSkullType().ordinal()));
			root.set("facing", new NBTTagInt("",((Skull)state).getRotation().ordinal()));
			
			if(((Skull)state).hasOwner())
				root.set("owner", new NBTTagString("", ((Skull)state).getOwner()));
		}
		else if(state instanceof BrewingStand)
		{
			root.set("type", new NBTTagString("","brewingStand"));
			
			root.set("brewTime", new NBTTagInt("",((BrewingStand)state).getBrewingTime()));
		}
		else if(state instanceof InventoryHolder)
		{
			root.set("type", new NBTTagString("","inventory"));
		}
		else
			return null;
		
		
		if(state instanceof InventoryHolder)
		{
			Inventory inv = null;
			if(state instanceof Chest)
				inv = ((Chest)state).getBlockInventory();
			else
				inv = ((InventoryHolder)state).getInventory();
			
			NBTTagList items = new NBTTagList("");
			
			for(int i = 0; i < inv.getContents().length; ++i)
			{
				if(inv.getContents()[i] != null)
				{
					NBTTagCompound item = new NBTTagCompound("");
					item.set("slot", new NBTTagInt("",i));
					
					au.com.mineauz.PlayerSpy.wrappers.minecraft.ItemStack nativeStack = Utility.convertToNative(inv.getContents()[i]);
					nativeStack.writeToNBT(item);
					items.add(item);
				}
			}
			
			root.set("inventory", items);
		}
		
		return root;
	}
	
	private void applyStoredState(BlockState state, NBTTagCompound data) throws RecordFormatException
	{
		if(data == null)
			return;
		
		if(data.getString("type").equals("sign"))
		{
			if(!(state instanceof Sign))
				throw new RecordFormatException("Incorrect BlockState type found. Expected: Sign Found: " + state.getClass().getSimpleName());
			
			NBTTagList lines = data.getList("lines");
			if(lines.size() != 4)
				throw new RecordFormatException("Expected 4 lines for sign text.");
			
			for(int i = 0; i < 4; ++i)
				((Sign)state).setLine(i, ((NBTTagString)lines.get(i)).getData());
		}
		else if(data.getString("type").equals("spawner"))
		{
			if(!(state instanceof CreatureSpawner))
				throw new RecordFormatException("Incorrect BlockState type found. Expected: CreatureSpawner Found: " + state.getClass().getSimpleName());
			
			((CreatureSpawner)state).setCreatureTypeByName(data.getString("spawnType"));
			((CreatureSpawner)state).setDelay(data.getInt("delay"));
		}
		else if(data.getString("type").equals("jukebox"))
		{
			if(!(state instanceof Jukebox))
				throw new RecordFormatException("Incorrect BlockState type found. Expected: Jukebox Found: " + state.getClass().getSimpleName());
			
			((Jukebox)state).setPlaying(Material.getMaterial(data.getInt("disc")));
		}
		else if(data.getString("type").equals("noteblock"))
		{
			if(!(state instanceof NoteBlock))
				throw new RecordFormatException("Incorrect BlockState type found. Expected: NoteBlock Found: " + state.getClass().getSimpleName());
			
			((NoteBlock)state).setRawNote(data.getByte("note"));
		}
		else if(data.getString("type").equals("skull"))
		{
			if(!(state instanceof Skull))
				throw new RecordFormatException("Incorrect BlockState type found. Expected: Skull Found: " + state.getClass().getSimpleName());
			
			((Skull)state).setSkullType(SkullType.values()[data.getInt("skullType")]);
			((Skull)state).setRotation(BlockFace.values()[data.getInt("facing")]);
			
			
			if(data.hasKey("owner"))
				((Skull)state).setOwner(data.getString("owner"));
		}
		else if(data.getString("type").equals("brewingstand"))
		{
			if(!(state instanceof BrewingStand))
				throw new RecordFormatException("Incorrect BlockState type found. Expected: BrewingStand Found: " + state.getClass().getSimpleName());
			
			((BrewingStand)state).setBrewingTime(data.getInt("brewTime"));
		}
		
		if(data.hasKey("inventory"))
		{
			if(!(state instanceof InventoryHolder))
				throw new RecordFormatException("Incorrect BlockState type found. Expected: InventoryHolder Found: " + state.getClass().getSimpleName());
			
			Inventory inv = null;
			if(state instanceof Chest)
				inv = ((Chest)state).getBlockInventory();
			else
				
				inv = ((InventoryHolder)state).getInventory();
			
			NBTTagList items = data.getList("inventory");
			
			ItemStack[] stacks = new ItemStack[inv.getContents().length];
			
			for(int i = 0; i < items.size(); ++i)
			{
				NBTTagCompound item = (NBTTagCompound)items.get(i);

				au.com.mineauz.PlayerSpy.wrappers.minecraft.ItemStack nativeStack = new au.com.mineauz.PlayerSpy.wrappers.minecraft.ItemStack(0, 0, 0);
				nativeStack.readFromNBT(item);
				
				int slot = item.getInt("slot");
				ItemStack stack = Utility.convertToBukkit(nativeStack);
				
				stacks[slot] = stack;
			}
			
			inv.setContents(stacks);
			
			
		}
		
	}
	
	private void initFromBlockState(BlockState block)
	{
		if(block == null)
		{
			mLocation = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
			mType = Material.AIR;
			mData = 0;
			mStateData = null;
			return;
		}
		mLocation = block.getLocation().clone();
		mType = block.getType();
		mData = block.getRawData();
		
		mStateData = stateToTagCompound(block);
	}
	
	public StoredBlock(Block block)
	{
		if(block == null)
		{
			mLocation = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
			mType = Material.AIR;
			mData = 0;
			mStateData = null;
			return;
		}
		initFromBlockState(block.getState());
	}
	public StoredBlock(Location location, Material type, byte data)
	{
		mLocation = location.clone();
		mType = type;
		mData = data;
		mStateData = null;
	}
	public StoredBlock()
	{
		mStateData = null;
	}
	
	public StoredBlock(BlockState block) 
	{
		initFromBlockState(block);
	}
	public StoredBlock(MaterialData data, Location location) 
	{
		mLocation = location.clone();
		mType = data.getItemType();
		mData = data.getData();
		mStateData = null;
	}
	public Location getLocation()
	{
		return mLocation;
	}
	
	public Material getType()
	{
		return mType;
	}
	public int getTypeId()
	{
		return mType.getId();
	}
	public byte getData()
	{
		return mData;
	}

	public void applyBlockInWorld() throws RecordFormatException
	{
		mLocation.getBlock().setTypeIdAndData(mType.getId(), mData, false);
		
		BlockState state = mLocation.getBlock().getState();
		
		applyStoredState(state, mStateData);
		
		state.update(true);
	}
	public void write(DataOutputStream stream, boolean absolute, boolean full) throws IOException
	{
		stream.writeInt(getTypeId());
		stream.writeByte(mData);
		
		new StoredLocation(mLocation).writeLocation(stream, absolute);
		
		if(mStateData == null || !full)
			stream.writeByte(0);
		else
		{
			stream.writeByte(1);
			
			NBTCompressedStreamTools.writeCompressed(mStateData, stream);
		}
	}
	
	public void read(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		int typeId = stream.readInt();
		mType = Material.getMaterial(typeId);
		mData = stream.readByte();
		
		if(mType == null)
			throw new RecordFormatException("Bad material type " + typeId);
		
		if(absolute)
			mLocation = StoredLocation.readLocationFull(stream).getLocation();
		else
			mLocation = StoredLocation.readLocation(stream, currentWorld).getLocation();
		
		if(stream.readByte() == 1)
		{
			mStateData = NBTCompressedStreamTools.readCompressed(stream);
		}
	}
	
	public int getSize(boolean absolute, boolean full)
	{
		int size = 6 + new StoredLocation(mLocation).getSize(absolute);

		if(mStateData == null || !full)
			return size;
		
		ByteArrayOutputStream temp = new ByteArrayOutputStream();
		NBTCompressedStreamTools.writeCompressed(mStateData, temp);
		
		return size + temp.size();
	}
	
	@Override
	public String toString()
	{
		return "Block: " + mType.toString() + ":" + mData + " hasTag:" + (mStateData != null);
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof StoredBlock))
			return false;
		
		StoredBlock block = (StoredBlock)obj;
		
		if(mType != block.mType || mData != block.mData)
			return false;
		
		if(!mLocation.equals(block.mLocation))
			return false;
		
		return mStateData.equals(block.mStateData);
	}
}
