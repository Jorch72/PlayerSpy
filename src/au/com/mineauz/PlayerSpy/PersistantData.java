package au.com.mineauz.PlayerSpy;

import java.io.File;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;

import au.com.mineauz.PlayerSpy.Utilities.AutoConfig;
import au.com.mineauz.PlayerSpy.Utilities.ConfigField;

public class PersistantData extends AutoConfig
{
	public PersistantData( File file )
	{
		super(file);
	}

	@ConfigField(name="activeMonitorTargets")
	private String[] mActiveMonitorTargets = new String[0];
	
	public ArrayList<OfflinePlayer> activeMonitorTargets;
	
	@Override
	protected void onPostLoad() throws InvalidConfigurationException
	{
		activeMonitorTargets = new ArrayList<OfflinePlayer>();
		for(String target : mActiveMonitorTargets)
			activeMonitorTargets.add(Bukkit.getOfflinePlayer(target));
	}
	
	@Override
	protected void onPreSave()
	{
		mActiveMonitorTargets = new String[activeMonitorTargets.size()];
		for(int i = 0; i < activeMonitorTargets.size(); i++)
			mActiveMonitorTargets[i] = activeMonitorTargets.get(i).getName();
	}
}
