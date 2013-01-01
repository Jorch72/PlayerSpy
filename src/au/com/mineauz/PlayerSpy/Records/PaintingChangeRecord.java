package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Painting;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.storage.StoredPainting;

public class PaintingChangeRecord extends Record implements IRollbackable, ILocationAware
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
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
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
		String name = ChatColor.DARK_AQUA + Utility.formatItemName(new ItemStack(Material.PAINTING)) + ChatColor.RESET;
		if(mPlaced)
			return "%s placed a " + name;
		else
			return "%s removed a " + name;
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
	@Override
	public Location getLocation()
	{
		return mPainting.getLocation();
	}
}
