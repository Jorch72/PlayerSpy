package au.com.mineauz.PlayerSpy;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UTFDataFormatException;

import org.bukkit.SkullType;
import org.bukkit.block.BlockFace;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class SkullData
{
	public SkullType type;
	public String player;
	public BlockFace facing;
	
	public void write(DataOutput output) throws IOException
	{
		output.writeByte(type.ordinal());
		if(type == SkullType.PLAYER)
			output.writeUTF(player);
		
		output.writeByte(facing.ordinal());
	}
	
	public void read(DataInput input) throws IOException, RecordFormatException
	{
		int typeId = input.readByte();
		if(typeId < 0 || typeId >= SkullType.values().length)
			throw new RecordFormatException("Bad skull type " + typeId);
		
		type = SkullType.values()[typeId];
		
		try
		{
			if(type == SkullType.PLAYER)
				player = input.readUTF();
		}
		catch(UTFDataFormatException e)
		{
			throw new RecordFormatException("Error reading UTF string. Malformed data.");
		}
		
		int facingId = input.readByte();
		
		if(facingId < 0 || facingId >= BlockFace.values().length)
			throw new RecordFormatException("Bad facing direction " + facingId);
		
		facing = BlockFace.values()[facingId];
	}
	
	public int getSize()
	{
		return 2 + (type == SkullType.PLAYER ? Utility.getUTFLength(player) : 0);
	}
}
