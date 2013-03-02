package au.com.mineauz.PlayerSpy.inspect;

import au.com.mineauz.PlayerSpy.SpyPlugin;

public class InspectInfo
{
	public long lastInspectTime;
	public boolean showEntities;
	public boolean showUse;
	public boolean showItems;
	public boolean showBlocks;
	
	public int itemCount;
	
	public void loadDefaults()
	{
		itemCount = SpyPlugin.getSettings().inspectCount;
		
		showEntities = SpyPlugin.getSettings().inspectEntities;
		showUse = SpyPlugin.getSettings().inspectUse;
		showItems = SpyPlugin.getSettings().inspectTransactions;
		showBlocks = true;
	}
}
