package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;


public class SprintRecord extends Record 
{
	public SprintRecord(boolean enabled) 
	{
		super(RecordType.Sprint);
		mEnabled = enabled;
	}
	public SprintRecord()
	{
		super(RecordType.Sprint);
	}

	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		stream.writeBoolean(mEnabled);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mEnabled = stream.readBoolean();
	}

	public boolean isEnabled()
	{
		return mEnabled;
	}
	
	private boolean mEnabled;

	@Override
	protected int getContentSize() 
	{
		return 1;
	}

}
