package au.com.mineauz.PlayerSpy;

import java.util.TimeZone;

import org.bukkit.configuration.InvalidConfigurationException;

public class Config extends AutoConfig 
{
	@ConfigField(comment = "When set to true, records fluid flow as block changes, and traces the flow to find the cause.", category="tracing")
	public boolean recordFluidFlow = true;
	@ConfigField(comment = "When set to true, records mushroom spread as block changes, and traces the spread to find the cause.", category="tracing")
	public boolean recordMushroomSpread = true;
	@ConfigField(comment = "When set to true, records grass and mycel spread as block changes, and traces the spread to find the cause.", category="tracing")
	public boolean recordGrassSpread = true;
	@ConfigField(comment = "When set to true, records fire spread as block changes, and traces the spread to find the cause.", category="tracing")
	public boolean recordFireSpread = true;
	
	@ConfigField(name="timezone", comment = "Allows you to specify a timezone to display records with because java has a bug that means that sometimes, it may incorrectly get the timezone infomation from the system.", category="time")
	private String mTimezoneString = "UTC+10:00";
	public TimeZone timezone;
	
	@ConfigField(name="offset", comment = "Allows you to specify a time offset in standard date diff format.", category="time")
	private String mTimeOffsetString = "0s";
	public long timeoffset;

	@ConfigField(comment = "The number of items to display when you use the inspect tool on a block.", category="inspect")
	public int inspectCount = 3;
	@ConfigField(comment = "When set to true, transaction with inventories at the inspected block, will show in quick inspect.", category="inspect")
	public boolean inspectTransactions = true;
	@ConfigField(comment = "When set to true, use interactions with the inspected block, will show in quick inspect.", category="inspect")
	public boolean inspectUse = true;
	
	@ConfigField(name="logTimeout", comment = "The amount of time that must pass before logs are closed after being asked to close.\nA low value can create lag when someone disconnects and quickly reconnects as the system has to wait for the log to finish closing before it can open it again.\nA high value will mean that uneeded logs will be loaded for longer consuming ram.\nMust be specified in date diff format. Default is 20 seconds.", category="general")
	private String mLogTimeoutString = "20s";
	public long logTimeout;
	
	@ConfigField(comment = "You can set the language playerspy uses for displaying and reading item/block/entity names", category="general")
	public String language = "en_US";
	
	protected void onPreSave()
	{
		mTimeOffsetString = Util.dateDiffToString(timeoffset,true);
		mLogTimeoutString = Util.dateDiffToString(logTimeout,true);
		mTimezoneString = timezone.getID();
	}
	protected void onPostLoad() throws InvalidConfigurationException
	{
		timeoffset = Util.parseDateDiff(mTimeOffsetString);
		timezone = TimeZone.getTimeZone(mTimezoneString.replace("UTC", "GMT"));
		
		if(!StringTranslator.setActiveLanguage(language))
			throw new InvalidConfigurationException("Invalid language specified");
		
		logTimeout = Util.parseDateDiff(mLogTimeoutString);
	}
	
	
}
