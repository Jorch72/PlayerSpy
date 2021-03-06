package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;

public class SneakRecord extends Record 
{
	public SneakRecord(boolean enabled) 
	{
		super(RecordType.Sneak);
		mEnabled = enabled;
	}
	public SneakRecord()
	{
		super(RecordType.Sneak);
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeBoolean(mEnabled);
	}
	
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mEnabled = stream.readBoolean();
	}

	public boolean isEnabled()
	{
		return mEnabled;
	}
	
	private boolean mEnabled;

	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 1;
	}
	@Override
	public String getDescription()
	{
		return null;
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof SneakRecord))
			return false;
		
		return mEnabled == ((SneakRecord)obj).mEnabled;
	}
}
