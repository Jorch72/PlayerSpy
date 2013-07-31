package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;

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
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		try
		{
			mMessage = stream.readUTF();
			mPrevented = stream.readBoolean();
		}
		catch(UTFDataFormatException e)
		{
			throw new RecordFormatException("Error reading UTF string. Malformed data.");
		}
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
		return "%s: " + (mPrevented ? ChatColor.STRIKETHROUGH : "") + mMessage.replaceAll("%", "%%") + (mPrevented ? ChatColor.RESET : "");
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof ChatCommandRecord))
			return false;
		
		return (mMessage.equals(((ChatCommandRecord)obj).mMessage) && mPrevented == ((ChatCommandRecord)obj).mPrevented);
	}
	
	@Override
	public String toString()
	{
		return "ChatCommand" + (mPrevented ? "- : " : ": ") + '"' + mMessage + '"';
	}
}
