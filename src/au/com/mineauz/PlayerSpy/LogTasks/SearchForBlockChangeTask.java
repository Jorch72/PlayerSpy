package au.com.mineauz.PlayerSpy.LogTasks;

import org.bukkit.Material;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.storage.StoredBlock;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;

public class SearchForBlockChangeTask implements Task<Long>
{
	private final Material mBlockType;
	private final boolean mMined;
	private final long mSearchStartDate;
	private final boolean mForward;
	private final LogFile mLogFile;
	
	public SearchForBlockChangeTask(LogFile logFile, Material blockType, boolean mined, long searchStartDate, boolean forward)
	{
		mLogFile = logFile;
		mBlockType = blockType;
		mMined = mined;
		mSearchStartDate = searchStartDate;
		mForward = forward;
	}

	@Override
	public Long call() throws Exception 
	{
		// Load up the initial set
		RecordList temp;
		long date = mSearchStartDate;
		
		if(date == 0)
			date = (mForward ? mLogFile.getStartDate() + 1 : mLogFile.getEndDate() - 1);
		
		temp = mLogFile.loadRecordChunks(date, date);
		int startIndex = (mForward ? temp.getNextRecordAfter(date) : temp.getLastRecordBefore(date));
		
		while(temp.size() > 0)
		{
			if(mForward)
			{
				for(int i = startIndex; i < temp.size(); i++)
				{
					if(temp.get(i).getType() == RecordType.BlockChange)
					{
						if(matches((BlockChangeRecord)temp.get(i)))
							return temp.get(i).getTimestamp();
					}
				}
			}
			else
			{
				for(int i = startIndex; i >= 0; i--)
				{
					if(temp.get(i).getType() == RecordType.BlockChange)
					{
						if(matches((BlockChangeRecord)temp.get(i)))
							return temp.get(i).getTimestamp();
					}
				}
			}
			
			if(Thread.interrupted())
				return 0L;
			
			// Load the next chunk
			if(mForward)
				date = mLogFile.getNextAvailableDateAfter(temp.getEndTimestamp());
			else
				date = mLogFile.getNextAvailableDateBefore(temp.getStartTimestamp());
			
			temp = mLogFile.loadRecordChunks(date, date);
			
			// position the cursor
			if(mForward)
				startIndex = 0;
			else
				startIndex = temp.size()-1;
		}
		
		// No record was found
		return 0L;
	}
	
	private boolean matches(BlockChangeRecord record)
	{
		StoredBlock block;
		// Block break
		if(mMined && !record.mPlaced)
			block = record.getInitialBlock();
		// Block place
		else if(!mMined && record.mPlaced)
			block = record.getFinalBlock();
		else
			return false;
		
		Material actual = block.getType();
		
		// If you specify air, you match any block change
		if(mBlockType == Material.AIR)
			return true;
		
		if(actual == mBlockType)
			return true;
		
		// Match the similar ones
		if(mBlockType == Material.REDSTONE_ORE && (actual == Material.GLOWING_REDSTONE_ORE))
			return true;
		if(mBlockType == Material.REDSTONE_LAMP_ON && actual == Material.REDSTONE_LAMP_OFF)
			return true;
		if(mBlockType == Material.REDSTONE_TORCH_ON && actual == Material.REDSTONE_TORCH_OFF)
			return true;
		if(mBlockType == Material.DIODE_BLOCK_ON && actual == Material.DIODE_BLOCK_OFF)
			return true;
		if(mBlockType == Material.HUGE_MUSHROOM_1 && actual == Material.HUGE_MUSHROOM_2)
			return true;
		
		return false;
	}

	@Override
	public int getTaskTargetId()
	{
		return mLogFile.getName().hashCode();
	}
}
