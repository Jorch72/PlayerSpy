package au.com.mineauz.PlayerSpy.LogTasks;

import java.util.Arrays;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.InventoryViewer;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.InventoryRecord;
import au.com.mineauz.PlayerSpy.Records.RecordType;
import au.com.mineauz.PlayerSpy.Utilities.Util;
import au.com.mineauz.PlayerSpy.search.DateConstraint;
import au.com.mineauz.PlayerSpy.search.RecordTypeConstraint;
import au.com.mineauz.PlayerSpy.search.SearchFilter;
import au.com.mineauz.PlayerSpy.search.SearchResults;
import au.com.mineauz.PlayerSpy.search.SearchTask;

public class DisplayInventoryTask implements Task<Void>
{
	private OfflinePlayer mInvOwner;
	private CommandSender mSender;
	private long mDate;
	
	public DisplayInventoryTask(OfflinePlayer toDisplay, CommandSender sender, long date)
	{
		mInvOwner = toDisplay;
		mSender = sender;
		mDate = date;
	}
	@Override
	public Void call() throws Exception
	{
		// Find the records we are interested in
		SearchFilter filter = new SearchFilter();
		filter.causes.add(Cause.playerCause(mInvOwner));
		
		DateConstraint dc = new DateConstraint();
		dc.startDate = new Date(0);
		dc.endDate = new Date(mDate);
		filter.andConstraints.add(dc);
		
		filter.orConstraints.add(new RecordTypeConstraint(RecordType.FullInventory));
		filter.orConstraints.add(new RecordTypeConstraint(RecordType.UpdateInventory));
		
		SearchTask task = new SearchTask(filter);
		SearchResults results = task.call();
		
		// We will use the record list container because it offers a nice way to get the current inventory
		RecordList rlist = new RecordList();
		for(int i = results.allRecords.size() - 1; i >= 0; --i)
			rlist.add(results.allRecords.get(i).getArg1());
		
		InventoryRecord currentInventory = rlist.getCurrentInventory(rlist.size()-1);
		if(currentInventory == null)
		{
			mSender.sendMessage(ChatColor.RED + "There is no inventory data for " + mInvOwner.getName() + " at " + Util.dateToString(mDate));
			return null;
		}
		// Build a bukkit inventory
		Inventory inventory = Bukkit.createInventory(null, 45, mInvOwner.getName() + " - " + Util.dateToString(mDate));
		
		ItemStack[] inv = Arrays.copyOf(currentInventory.getItems(), 40);
		for(int i = 0; i < 4; ++i)
			inv[36+i] = currentInventory.getArmour()[i];
		
		inventory.setContents(currentInventory.getItems());
		
		// Display it
		if(mSender instanceof Player)
			InventoryViewer.openInventory(inventory, (Player)mSender, null, false);
		else
			InventoryViewer.printInventory(inventory, mSender);
		
		return null;
	}

	@Override
	public int getTaskTargetId()
	{
		return -1;
	}

}
