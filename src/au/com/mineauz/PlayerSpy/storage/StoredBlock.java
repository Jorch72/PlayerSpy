package au.com.mineauz.PlayerSpy.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Jukebox;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.entity.EntityType;
import org.bukkit.material.MaterialData;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;

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
		Spawner,
		// SkullData instance
		Skull
	}
	
	private Location mLocation;
	private Material mType;
	private byte mData;
	private BlockStateType mStateType;
	private Object mStateData;
	
	private void initFromBlockState(BlockState block)
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
			try
			{
				mStateData = ((Jukebox)block).getPlaying();
			}
			catch(NullPointerException e) // Workaround to Craftbukkit bug(it doesnt check if there is a record before getting its id)
			{
				mStateData = Material.AIR;
			}
		}
		else if(block instanceof CreatureSpawner)
		{
			mStateType = BlockStateType.Spawner;
			mStateData = ((CreatureSpawner)block).getSpawnedType();
		}
		else if(block instanceof Skull)
		{
			mStateType = BlockStateType.Skull;
			SkullData data = new SkullData();
			data.type = ((Skull)block).getSkullType();
			
			if(data.type == SkullType.PLAYER)
				data.player = ((Skull)block).getOwner();
			
			data.facing = ((Skull)block).getRotation();
			
			mStateData = data;
		}
		else
			mStateType = BlockStateType.NormalBlock;
	}
	
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
		initFromBlockState(block.getState());
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
		initFromBlockState(block);
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
	
	public void applyBlockInWorld()
	{
		mLocation.getBlock().setTypeIdAndData(mType.getId(), mData, false);
		
		BlockState state = mLocation.getBlock().getState();
		
		switch(mStateType)
		{
		case Sign:
			for(int i = 0; i < 4; i++)
				((Sign)state).setLine(i, ((String[])mStateData)[i]);
			break;
		case Jukebox:
			((Jukebox)state).setPlaying((Material)mStateData);
			break;
		case NoteBlock:
			((NoteBlock)state).setNote((Note)mStateData);
			break;
		case Spawner:
			((CreatureSpawner)state).setSpawnedType((EntityType)mStateData);
			break;
		case Skull:
		{
			SkullData data = (SkullData)mStateData;
			((Skull)state).setSkullType(data.type);
			((Skull)state).setRotation(data.facing);
			((Skull)state).setOwner(data.player);
			break;
		}
		case NormalBlock:
			break;
		}
		
		state.update(true);
	}
	public void write(DataOutputStream stream, boolean absolute) throws IOException
	{
		stream.writeInt(getTypeId());
		stream.writeByte(mData);
		
		new StoredLocation(mLocation).writeLocation(stream, absolute);
		
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
		case Skull:
			((SkullData)mStateData).write(stream);
			break;
		default:
			break;
		}
	}
	
	public void read(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		mType = Material.getMaterial(stream.readInt());
		mData = stream.readByte();
		
		if(mType == null)
			throw new RecordFormatException("Bad material type");
		
		if(absolute)
			mLocation = StoredLocation.readLocationFull(stream).getLocation();
		else
			mLocation = StoredLocation.readLocation(stream, currentWorld).getLocation();
		
		int stateTypeInt = stream.readByte();
		if(stateTypeInt < 0 || stateTypeInt >= BlockStateType.values().length)
			throw new RecordFormatException("Block state type out of range");
		
		try
		{
			mStateType = BlockStateType.values()[stateTypeInt];
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
				if(mStateData == null)
					throw new RecordFormatException("Bad material type for jukebox disk");
				break;
			case NoteBlock:
				int note = stream.readByte();
				if(note < 0 || note > 24)
					throw new RecordFormatException("Bad note " + note + " read");
				mStateData = new Note(stream.readByte());
				break;
			case Spawner:
				mStateData = EntityType.fromId(stream.readShort());
				if(mStateData == null)
					throw new RecordFormatException("Bad entity type for monster spawner");
				break;
			case Skull:
				mStateData = new SkullData();
				
				((SkullData)mStateData).read(stream);
				break;
			default:
				mStateData = null;
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
		int size = 6 + new StoredLocation(mLocation).getSize(absolute);
		
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
		case Skull:
			size += ((SkullData)mStateData).getSize();
			break;
		default:
			break;
		}
		
		return size;
	}
}