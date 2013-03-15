package au.com.mineauz.PlayerSpy;

import java.io.File;
import java.util.TimeZone;

import org.bukkit.configuration.InvalidConfigurationException;

import au.com.mineauz.PlayerSpy.Utilities.AutoConfig;
import au.com.mineauz.PlayerSpy.Utilities.ConfigField;
import au.com.mineauz.PlayerSpy.Utilities.StringTranslator;
import au.com.mineauz.PlayerSpy.Utilities.Util;

public class Config extends AutoConfig 
{
	@ConfigField(comment = "DISABLED FOR NOW. When set to true, records fluid flow as block changes, and traces the flow to find the cause.", category="tracing")
	public boolean recordFluidFlow = false;
	@ConfigField(comment = "DISABLED FOR NOW. When set to true, records mushroom spread as block changes, and traces the spread to find the cause.", category="tracing")
	public boolean recordMushroomSpread = false;
	@ConfigField(comment = "DISABLED FOR NOW. When set to true, records grass and mycel spread as block changes, and traces the spread to find the cause.", category="tracing")
	public boolean recordGrassSpread = false;
	@ConfigField(comment = "DISABLED FOR NOW. When set to true, records fire spread as block changes, and traces the spread to find the cause.", category="tracing")
	public boolean recordFireSpread = false;
	
	@ConfigField(name="timezone", comment = "Allows you to specify a timezone to display records with because java has a bug that means that sometimes, it may incorrectly get the timezone infomation from the system.", category="time")
	private String mTimezoneString = "UTC+10:00";
	public TimeZone timezone;

	@ConfigField(comment = "The number of items to display when you use the inspect tool on a block.", category="inspect")
	public int inspectCount = 3;
	@ConfigField(comment = "When set to true, transaction with inventories at the inspected block, will show in quick inspect.", category="inspect")
	public boolean inspectTransactions = true;
	@ConfigField(comment = "When set to true, use interactions with the inspected block, will show in quick inspect.", category="inspect")
	public boolean inspectUse = true;
	@ConfigField(comment = "When set to true, any entity related records at the inspected location, will in quick inspect.", category="inspect")
	public boolean inspectEntities = true;
	
	@ConfigField(comment = "The timeout in miliseconds between inspects. If you are getting double clicks while inspecting, increase this.", category="inspect")
	public long inspectTimeout = 500;
	
	@ConfigField(name="logTimeout", comment = "The amount of time that must pass before logs are closed after being asked to close.\nA low value can create lag when someone disconnects and quickly reconnects as the system has to wait for the log to finish closing before it can open it again.\nA high value will mean that uneeded logs will be loaded for longer consuming ram.\nMust be specified in date diff format. Default is 20 seconds.", category="general")
	private String mLogTimeoutString = "20s";
	public long logTimeout;
	
	@ConfigField(comment = "You can set the language playerspy uses for displaying and reading item/block/entity names", category="general")
	public String language = "en_US";
	
	@ConfigField(comment = "The maximum number of results that can be retrieved with search or history.", category="general")
	public int maxSearchResults = 1000;
	
	@ConfigField(comment = "The maximum number of changes (block or transactions etc) that will be done each tick.", category="rollback")
	public int maxChangesPerTick = 10;
	
	@ConfigField(name="catchupTime", comment="The amount of time the catchup command looks back.\nThis should be in standard date-diff format.", category="general")
	private String mCatchupTime = "10m";
	
	public long catchupTime;	
	
	@ConfigField(comment="The format that chat through the catchup command will show using. %name will be replaced with the player name. %message will be replaced with the contents of the message.")
	public String chatFormat = "<%name> %message";
	
	public Config(File file)
	{
		super(file);
	}
	
	protected void onPreSave()
	{
		//mTimeOffsetString = Util.dateDiffToString(timeoffset,true);
		mLogTimeoutString = Util.dateDiffToString(logTimeout,true);
		mTimezoneString = timezone.getID();
		
		mCatchupTime = Util.dateDiffToString(catchupTime, true);
	}
	protected void onPostLoad() throws InvalidConfigurationException
	{
		//timeoffset = Util.parseDateDiff(mTimeOffsetString);
		timezone = TimeZone.getTimeZone(mTimezoneString.replace("UTC", "GMT"));
		
		if(!StringTranslator.setActiveLanguage(language))
			throw new InvalidConfigurationException("Invalid language specified");
		
		logTimeout = Util.parseDateDiff(mLogTimeoutString);
		
		catchupTime = Util.parseDateDiff(mCatchupTime);
	}
	
	
}
