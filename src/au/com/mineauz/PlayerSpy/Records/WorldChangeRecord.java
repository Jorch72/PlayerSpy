package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.*;

public class WorldChangeRecord extends Record {

	public WorldChangeRecord(World world) 
	{
		super(RecordType.WorldChange);
		mWorld = world;
	}
	public WorldChangeRecord() 
	{
		super(RecordType.WorldChange);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeUTF(mWorld.getName());
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		String worldName = stream.readUTF();
		mWorld = Bukkit.getWorld(worldName);
		if(mWorld == null)
		{
			mWorld = Bukkit.getWorlds().get(0);
			LogUtil.warning("Invalid world '" + worldName + "'in record. Defaulting to '" + mWorld.getName() + "'. Did you delete a world?");
		}
	}

	public World getWorld()
	{
		return mWorld;
	}
	private World mWorld;
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 2 + mWorld.getName().length();
	}
}
