package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.GameMode;
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
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeByte(mGameMode);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mGameMode = stream.readByte();
	}
	
	public int getGameMode()
	{
		return mGameMode;
	}
	private int mGameMode;
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 1;
	}
	@Override
	public String getDescription()
	{
		return "Gamemode changed to " + GameMode.values()[mGameMode].toString() + " for %s";
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof GameModeRecord))
			return false;
		
		return mGameMode == ((GameModeRecord)obj).mGameMode;
	}
}
