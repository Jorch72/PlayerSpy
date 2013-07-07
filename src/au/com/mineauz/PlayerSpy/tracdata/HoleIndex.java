package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;

import au.com.mineauz.PlayerSpy.structurefile.AbstractHoleIndex;
import au.com.mineauz.PlayerSpy.structurefile.SpaceLocator;

public class HoleIndex extends AbstractHoleIndex
{
	private FileHeader mHeader;
	
	public HoleIndex( LogFile log, FileHeader header, RandomAccessFile file, SpaceLocator locator )
	{
		super(log, file, locator);
		mHeader = header;
	}

	@Override
	public long getLocation()
	{
		return mHeader.HolesIndexLocation;
	}

	@Override
	public long getSize()
	{
		return mHeader.HolesIndexSize;
	}

	@Override
	protected int getElementCount()
	{
		return mHeader.HolesIndexCount;
	}

	@Override
	protected void updateElementCount( int newCount )
	{
		mHeader.HolesIndexCount = newCount;
	}

	@Override
	protected void updateSize( long newSize )
	{
		mHeader.HolesIndexSize = newSize;
	}

	@Override
	protected void updateLocation( long newLocation )
	{
		mHeader.HolesIndexLocation = newLocation;
	}
	
	@Override
	protected int getPadding()
	{
		return mHeader.HolesIndexPadding;
	}
	
	@Override
	protected void updatePadding( int newPadding )
	{
		mHeader.HolesIndexPadding = (short)newPadding;
	}
	
	@Override
	protected void saveChanges() throws IOException
	{
		mFile.seek(0);
		mHeader.write(mFile);
	}
}
