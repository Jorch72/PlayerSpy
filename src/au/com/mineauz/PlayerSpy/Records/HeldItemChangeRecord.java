package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;


public class HeldItemChangeRecord extends Record 
{

	public HeldItemChangeRecord(int newSlot) 
	{
		super(RecordType.HeldItemChange);
		mSlot = newSlot;
	}
	public HeldItemChangeRecord()
	{
		super(RecordType.HeldItemChange);
	}

	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		stream.writeByte(mSlot);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mSlot = stream.readByte();
	}

	public int getSlot()
	{
		return mSlot;
	}
	private int mSlot;
	
	@Override
	protected int getContentSize() 
	{
		return 1;
	}

}
