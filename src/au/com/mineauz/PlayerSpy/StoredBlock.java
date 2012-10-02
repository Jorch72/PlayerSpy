package au.com.mineauz.PlayerSpy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Jukebox;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.material.MaterialData;

public class StoredBlock 
{
	public enum BlockStateType
	{
		// No extra data
		NormalBlock,
		// String[]
		Sign,
		// Note
		NoteBlock,
		// Material (the disk material)
		Jukebox,
		// EntityType (the type of entity spawned)
		Spawner
	}
	
	private Location mLocation;
	private Material mType;
	private byte mData;
	private BlockStateType mStateType;
	private Object mStateData;
	
	public StoredBlock(Block block)
	{
		if(block == null)
		{
			mLocation = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
			mType = Material.AIR;
			mData = 0;
			mStateType = BlockStateType.NormalBlock;
			return;
		}
		mLocation = block.getLocation().clone();
		mType = block.getType();
		mData = block.getData();
		
		if(block.getState() instanceof Sign)
		{
			mStateType = BlockStateType.Sign;
			mStateData = ((Sign)block.getState()).getLines();
		}
		else if(block.getState() instanceof NoteBlock)
		{
			mStateType = BlockStateType.NoteBlock;
			mStateData = ((NoteBlock)block.getState()).getNote();
		}
		else if(block.getState() instanceof Jukebox)
		{
			mStateType = BlockStateType.Jukebox;
			mStateData = ((Jukebox)block.getState()).getPlaying();
		}
		else if(block.getState() instanceof CreatureSpawner)
		{
			mStateType = BlockStateType.Spawner;
			mStateData = ((CreatureSpawner)block.getState()).getSpawnedType();
		}
		else
			mStateType = BlockStateType.NormalBlock;
	}
	public StoredBlock(Location location, Material type, byte data)
	{
		mLocation = location.clone();
		mType = type;
		mData = data;
		mStateType = BlockStateType.NormalBlock;
	}
	public StoredBlock()
	{
		mStateType = BlockStateType.NormalBlock;
	}
	
	public StoredBlock(BlockState block) 
	{
		if(block == null)
		{
			mLocation = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
			mType = Material.AIR;
			mData = 0;
			mStateType = BlockStateType.NormalBlock;
			return;
		}
		mLocation = block.getLocation().clone();
		mType = block.getType();
		mData = block.getRawData();
		
		if(block instanceof Sign)
		{
			mStateType = BlockStateType.Sign;
			mStateData = ((Sign)block).getLines();
		}
		else if(block instanceof NoteBlock)
		{
			mStateType = BlockStateType.NoteBlock;
			mStateData = ((NoteBlock)block).getNote();
		}
		else if(block instanceof Jukebox)
		{
			mStateType = BlockStateType.Jukebox;
			mStateData = ((Jukebox)block).getPlaying();
		}
		else if(block instanceof CreatureSpawner)
		{
			mStateType = BlockStateType.Spawner;
			mStateData = ((CreatureSpawner)block).getSpawnedType();
		}
		else
			mStateType = BlockStateType.NormalBlock;
	}
	public StoredBlock(MaterialData data, Location location) 
	{
		mLocation = location.clone();
		mType = data.getItemType();
		mData = data.getData();
		mStateType = BlockStateType.NormalBlock;
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
	
	public BlockStateType getStateType()
	{
		return mStateType;
	}
	
	public Object getStateData()
	{
		return mStateData;
	}
	
	public void write(DataOutputStream stream) throws IOException
	{
		stream.writeInt(getTypeId());
		stream.writeByte(mData);
		
		new StoredLocation(mLocation).writeLocation(stream, false);
		
		stream.writeByte((byte)mStateType.ordinal());
		switch(mStateType)
		{
		case Sign:
			stream.writeUTF(((String[])mStateData)[0]);
			stream.writeUTF(((String[])mStateData)[1]);
			stream.writeUTF(((String[])mStateData)[2]);
			stream.writeUTF(((String[])mStateData)[3]);
			break;
		case Jukebox:
			stream.writeInt(((Material)mStateData).getId());
			break;
		case NoteBlock:
			stream.writeByte(((Note)mStateData).getId());
			break;
		case Spawner:
			stream.writeShort(((EntityType)mStateData).getTypeId());
			break;
		default:
			break;
		}
	}
	
	public void read(DataInputStream stream, World currentWorld) throws IOException
	{
		mType = Material.getMaterial(stream.readInt());
		mData = stream.readByte();
		
		mLocation = StoredLocation.readLocation(stream, currentWorld).getLocation();
		mStateType = BlockStateType.values()[stream.readByte()];
		switch(mStateType)
		{
		case Sign:
			{
				String[] data = new String[4];
				data[0] = stream.readUTF();
				data[1] = stream.readUTF();
				data[2] = stream.readUTF();
				data[3] = stream.readUTF();
				mStateData = data;
				break;
			}
		case Jukebox:
			mStateData = Material.getMaterial(stream.readInt());
			break;
		case NoteBlock:
			mStateData = new Note(stream.readByte());
			break;
		case Spawner:
			mStateData = EntityType.fromId(stream.readShort());
			break;
		default:
			mStateData = null;
			break;
		}
	}
	
	public int getSize()
	{
		int size = 6 + new StoredLocation(mLocation).getSize(false);
		
		switch(mStateType)
		{
		case Sign:
			size += 8;
			size += ((String[])mStateData)[0].length();
			size += ((String[])mStateData)[1].length();
			size += ((String[])mStateData)[2].length();
			size += ((String[])mStateData)[3].length();
			break;
		case Jukebox:
			size += 4;
			break;
		case NoteBlock:
			size++;
			break;
		case Spawner:
			size += 2;
			break;
		default:
			break;
		}
		
		return size;
	}
}
