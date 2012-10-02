package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;

public class ArmSwingRecord extends Record 
{

	public ArmSwingRecord() 
	{
		super(RecordType.ArmSwing);
	}

	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		// Nothing to do
	}

	@Override
	protected int getContentSize() 
	{
		return 0;
	}

	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		// Nothing to do
	}
}
