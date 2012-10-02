package au.com.mineauz.PlayerSpy.LogTasks;

import java.util.concurrent.Callable;

import org.bukkit.Material;

import au.com.mineauz.PlayerSpy.InventorySlot;
import au.com.mineauz.PlayerSpy.LogFile;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.InventoryRecord;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Records.UpdateInventoryRecord;

public class SearchForItemTask implements Callable<Long>
{
	private final Material mItemType;
	private final boolean mGained;
	private final long mSearchStartDate;
	private final boolean mForward;
	private final LogFile mLogFile;
	
	public SearchForItemTask(LogFile logFile, Material itemType, boolean gained, long searchStartDate, boolean forward)
	{
		mLogFile = logFile;
		mItemType = itemType;
		mGained = gained;
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
		
		InventoryRecord lastInventory = temp.getCurrentInventory(startIndex);
		
		while(temp.size() > 0)
		{
			if(mForward)
			{
				for(int i = startIndex; i < temp.size(); i++)
				{
					if(temp.get(i).getType() == RecordType.FullInventory || temp.get(i).getType() == RecordType.UpdateInventory)
					{
						if(lastInventory == null && temp.get(i).getType() == RecordType.FullInventory)
							lastInventory = (InventoryRecord)temp.get(i);
						else if(matches(temp.get(i), lastInventory))
							return temp.get(i).getTimestamp();
					}
				}
			}
			else
			{
				for(int i = startIndex; i >= 0; i--)
				{
					if(temp.get(i).getType() == RecordType.FullInventory || temp.get(i).getType() == RecordType.UpdateInventory)
					{
						if(lastInventory == null && temp.get(i).getType() == RecordType.FullInventory)
							lastInventory = (InventoryRecord)temp.get(i);
						else if(matches(temp.get(i), lastInventory))
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
			
			lastInventory = temp.getCurrentInventory(startIndex);
		}
		
		// No record was found
		return 0L;
	}
	
	private boolean matches(Record record, InventoryRecord lastInventory)
	{
		if(record.getType() == RecordType.FullInventory)
		{
			InventoryRecord irecord = (InventoryRecord)record;
			if(mGained)
			{
				for(int i = 0; i < lastInventory.getItems().length; i++)
				{
					if(lastInventory.getItems()[i] == null && irecord.getItems()[i] != null)
					{
						if(irecord.getItems()[i].getType() == mItemType)
							return true;
					}
					else if(lastInventory.getItems()[i] != null && irecord.getItems()[i] != null)
					{
						if(lastInventory.getItems()[i].getType() != mItemType && irecord.getItems()[i].getType() == mItemType)
							return true;
					}
				}
				
				for(int i = 0; i < lastInventory.getArmour().length; i++)
				{
					if(lastInventory.getArmour()[i] == null && irecord.getArmour()[i] != null)
					{
						if(irecord.getArmour()[i].getType() == mItemType)
							return true;
					}
					else if(lastInventory.getArmour()[i] != null && irecord.getArmour()[i] != null)
					{
						if(lastInventory.getArmour()[i].getType() != mItemType && irecord.getArmour()[i].getType() == mItemType)
							return true;
					}
				}
			}
			else
			{
				for(int i = 0; i < lastInventory.getItems().length; i++)
				{
					if(lastInventory.getItems()[i] != null && irecord.getItems()[i] == null)
					{
						if(lastInventory.getItems()[i].getType() == mItemType)
							return true;
					}
					else if(lastInventory.getItems()[i] != null && irecord.getItems()[i] != null)
					{
						if(lastInventory.getItems()[i].getType() == mItemType && irecord.getItems()[i].getType() != mItemType)
							return true;
					}
				}
				
				for(int i = 0; i < lastInventory.getArmour().length; i++)
				{
					if(lastInventory.getArmour()[i] != null && irecord.getArmour()[i] == null)
					{
						if(lastInventory.getArmour()[i].getType() == mItemType)
							return true;
					}
					else if(lastInventory.getArmour()[i] != null && irecord.getArmour()[i] != null)
					{
						if(lastInventory.getArmour()[i].getType() == mItemType && irecord.getArmour()[i].getType() != mItemType)
							return true;
					}
				}
			}
			
			// Copy the inv
			for(int i = 0; i < irecord.getItems().length; i++)
			{
				lastInventory.getItems()[i] = irecord.getItems()[i];
			}
			
			for(int i = 0; i < irecord.getArmour().length; i++)
			{
				lastInventory.getArmour()[i] = irecord.getArmour()[i];
			}
		}
		else
		{
			UpdateInventoryRecord urecord = (UpdateInventoryRecord)record;
			for(InventorySlot slot : urecord.Slots)
			{
				if(slot.Item.getType() == mItemType && mGained)
					return true;
				else if(!mGained && slot.Item.getType() != mItemType)
				{
					if(slot.Slot > lastInventory.getItems().length)
					{
						if(lastInventory.getArmour()[slot.Slot - lastInventory.getItems().length].getType() == mItemType)
							return true;
						lastInventory.getArmour()[slot.Slot -lastInventory.getItems().length] = slot.Item;
					}
					else if(lastInventory.getItems()[slot.Slot].getType() == mItemType)
						return true;
					else
						lastInventory.getItems()[slot.Slot] = slot.Item;
				}
					
			}
		}
		return false;
	}
}
