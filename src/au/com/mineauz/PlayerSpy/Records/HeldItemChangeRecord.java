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
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeByte(mSlot);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mSlot = stream.readByte();
	}

	public int getSlot()
	{
		return mSlot;
	}
	private int mSlot;
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return 1;
	}
	@Override
	public String getDescription()
	{
		return "%s changed held item to slot " + mSlot;
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof HeldItemChangeRecord))
			return false;
		
		return (mSlot == ((HeldItemChangeRecord)obj).mSlot);
	}

}
