package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;

public class ChatCommandRecord extends Record 
{

	public ChatCommandRecord(String message, boolean prevented) 
	{
		super(RecordType.ChatCommand);
		mMessage = message;
		mPrevented = prevented;
	}
	public ChatCommandRecord()
	{
		super(RecordType.ChatCommand);
	}
	@Override
	protected void writeContents(DataOutputStream stream) throws IOException 
	{
		stream.writeUTF(mMessage);
		stream.writeBoolean(mPrevented);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld) throws IOException 
	{
		mMessage = stream.readUTF();
		mPrevented = stream.readBoolean();
	}
	
	public String getMessage()
	{
		return mMessage;
	}
	
	private String mMessage;
	private boolean mPrevented;
	@Override
	protected int getContentSize() 
	{
		return 3 + mMessage.length();
	}
}
