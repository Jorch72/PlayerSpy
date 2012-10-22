package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.World;

import au.com.mineauz.PlayerSpy.Utilities.Utility;

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
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeUTF(mMessage);
		stream.writeBoolean(mPrevented);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
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
	protected int getContentSize(boolean absolute) 
	{
		return 1 + Utility.getUTFLength(mMessage);
	}
	@Override
	public String getDescription()
	{
		return "%s: " + (mPrevented ? ChatColor.STRIKETHROUGH : "") + mMessage + (mPrevented ? ChatColor.RESET : "");
	}
}
