package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class WorldChangeRecord extends Record {

	public WorldChangeRecord(World world) 
	{
		super(RecordType.WorldChange);
		mWorld = world;
		mWorldString = world.getName();
	}
	public WorldChangeRecord() 
	{
		super(RecordType.WorldChange);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeUTF(mWorldString);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mWorldString = stream.readUTF();
		mWorld = Bukkit.getWorld(mWorldString);
		if(mWorld == null)
		{
			mWorld = Bukkit.getWorlds().get(0);
			//LogUtil.warning("Invalid world '" + mWorldString + "'in record. Defaulting to '" + mWorld.getName() + "'. Did you delete a world?");
		}
	}

	public World getWorld()
	{
		return mWorld;
	}
	private World mWorld;
	private String mWorldString;
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return Utility.getUTFLength(mWorldString);
	}
	@Override
	public String getDescription()
	{
		return "%s changed to " + ChatColor.DARK_AQUA + mWorldString + ChatColor.RESET;
	}
}
