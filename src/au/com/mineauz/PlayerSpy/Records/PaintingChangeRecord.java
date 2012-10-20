package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.entity.Painting;

import au.com.mineauz.PlayerSpy.StoredPainting;

public class PaintingChangeRecord extends Record implements IRollbackable
{

	public PaintingChangeRecord(Painting painting, boolean place) 
	{
		super(RecordType.PaintingChange);
		mPainting = new StoredPainting(painting);
		mPlaced = place;
		mIsRolledBack = false;
	}
	public PaintingChangeRecord()
	{
		super(RecordType.PaintingChange);
		mIsRolledBack = false;
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeBoolean(mPlaced);
		mPainting.writePainting(stream, absolute);
		stream.writeBoolean(mIsRolledBack);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException 
	{
		mPlaced = stream.readBoolean();
		
		mPainting = StoredPainting.readPainting(stream, currentWorld, absolute);
		mIsRolledBack = stream.readBoolean();
	}
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return mPainting.getSize(absolute) + 2; 
	}

	public StoredPainting getPainting()
	{
		return mPainting;
	}
	public boolean getPlaced()
	{
		return mPlaced;
	}
	
	private StoredPainting mPainting;
	private boolean mPlaced;
	private boolean mIsRolledBack;
	
	@Override
	public String getDescription()
	{
		if(mPlaced)
			return "%s placed a painting";
		else
			return "%s removed a painting";
	}
	@Override
	public boolean canBeRolledBack()
	{
		return true;
	}
	@Override
	public boolean wasRolledBack()
	{
		return mIsRolledBack;
	}
	@Override
	public void setRolledBack( boolean value )
	{
		mIsRolledBack = value;
	}
}
