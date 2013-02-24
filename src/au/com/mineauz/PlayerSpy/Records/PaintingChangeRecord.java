package au.com.mineauz.PlayerSpy.Records;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Records.ILocationAware;
import au.com.mineauz.PlayerSpy.Records.IRollbackable;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Records.RecordType;
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
	@SuppressWarnings( "deprecation" )
	public PaintingChangeRecord(au.com.mineauz.PlayerSpy.legacy.v2.PaintingChangeRecord old)
	{
		super(RecordType.PaintingChange);
		mPainting = old.getPainting();
		mPlaced = old.getPlaced();
	}

	@Override
	protected void writeContents(DataOutputStream stream, boolean absolute) throws IOException 
	{
		stream.writeBoolean(mPlaced);
		mPainting.writePainting(stream, absolute);
	}
	@Override
	protected void readContents(DataInputStream stream, World currentWorld, boolean absolute) throws IOException, RecordFormatException
	{
		mPlaced = stream.readBoolean();
		
		mPainting = StoredPainting.readPainting(stream, currentWorld, absolute);
	}
	
	@Override
	protected int getContentSize(boolean absolute) 
	{
		return mPainting.getSize(absolute) + 1; 
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
	public boolean rollback( boolean preview, Player previewTarget )
	{
		if(!preview)
		{
			Painting painting = getLocation().getWorld().spawn(getLocation(), Painting.class);
			painting.setArt(mPainting.getArt());
			painting.setFacingDirection(mPainting.getBlockFace());
			
			return true;
		}
		return false;
	}
	@Override
	public boolean restore()
	{
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void setRollbackState( boolean state )
	{
		mIsRolledBack = state;
	}
	@Override
	public Location getLocation()
	{
		return mPainting.getLocation();
	}
}