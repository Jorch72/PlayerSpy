package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;

public class SessionInfoRecord extends Record 
{
	private boolean mDeep;
	public SessionInfoRecord(boolean deep) 
	{
		super(RecordType.EndOfSession);
		mDeep = deep;
	}
	public SessionInfoRecord()
	{
		super(RecordType.EndOfSession);
	}
	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeBoolean(mDeep);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mDeep = stream.readBoolean();
	}
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 1;
	}
	
	public boolean isDeep()
	{
		return mDeep;
	}
	@Override
	public String getDescription()
	{
		return null;
	}
	
}
