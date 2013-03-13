package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.ItemFrameChangeRecord;
import au.com.mineauz.PlayerSpy.Records.PaintingChangeRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class BlockChangeConstraint extends Constraint
{
	private boolean mRemove;
	private boolean mPlace;
	private boolean mPainting;
	private boolean mItemFrame;
	
	public BlockChangeConstraint(boolean remove, boolean place, boolean paintings, boolean itemframes)
	{
		mRemove = remove;
		mPlace = place;
		mPainting = paintings;
		mItemFrame = itemframes;
	}
	@Override
	public boolean matches( Record record )
	{
		if(record.getType() == RecordType.BlockChange)
		{
			if((mPlace && ((BlockChangeRecord)record).wasPlaced()) || (mRemove && !((BlockChangeRecord)record).wasPlaced()))
				return true;
		}
		else if(record.getType() == RecordType.PaintingChange && mPainting)
		{
			if((mPlace && ((PaintingChangeRecord)record).getPlaced()) || (mRemove && !((PaintingChangeRecord)record).getPlaced()))
				return true;
		}
		else if(record.getType() == RecordType.ItemFrameChange && mItemFrame)
		{
			if((mPlace && ((ItemFrameChangeRecord)record).getPlaced()) || (mRemove && !((ItemFrameChangeRecord)record).getPlaced()))
				return true;
		}
		return false;
	}

	@Override
	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
}
