package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;

public class GameModeRecord extends Record 
{

	public GameModeRecord(int gameMode) 
	{
		super(RecordType.GameMode);
		mGameMode = gameMode;
	}
	public GameModeRecord()
	{
		super(RecordType.GameMode);
	}
	
	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		stream.writeByte(mGameMode);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mGameMode = stream.readByte();
	}
	
	public int getGameMode()
	{
		return mGameMode;
	}
	private int mGameMode;
	
	@Override
	protected int getContentSize() 
	{
		return 1;
	}
}
