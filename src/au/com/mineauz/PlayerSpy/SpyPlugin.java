package au.com.mineauz.PlayerSpy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
//import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import au.com.mineauz.PlayerSpy.Upgrading.Upgrader;
import au.com.mineauz.PlayerSpy.Utilities.PriorityExecutor;
import au.com.mineauz.PlayerSpy.commands.*;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;
import au.com.mineauz.PlayerSpy.rollback.RollbackManager;
import au.com.mineauz.PlayerSpy.search.Searcher;

public class SpyPlugin extends JavaPlugin
{
	public void onEnable()
	{
		// Create the folder if needed
		if(!getDataFolder().exists())
			getDataFolder().mkdirs();
		
		LogUtil.setLogger(getLogger());
		//*****DEBUG******
		getLogger().setLevel(Level.FINE);
		//getLogger().getParent().getHandlers()[0].setLevel(Level.FINEST);
		//****END DEBUG****
		
		mConfig = new Config(new File(getDataFolder(), "config.yml"));
		// Load the config
		if(!getSettings().load())
			throw new RuntimeException("Failed to load configuration file. Cannot start PlayerSpy");
		getSettings().save();

		sPluginInstance = this;
		
		CommandDispatcher dispatch = new CommandDispatcher();
		getCommand("playerspy").setExecutor(dispatch);
		getCommand("playerspy").setTabCompleter(dispatch);
		
		// Update every tick
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

			@Override
			public void run() 
			{
				onTick();
			}
			
		}, 1L, 1L);

		LogFileRegistry.setLogFileDirectory(new File(getDataFolder(),"data"));
		if(!CrossReferenceIndex.instance.initialize())
			throw new RuntimeException("Failed to load database. Cannot start PlayerSpy");
		
		Upgrader.run();
		
		//addExistingMonitors();
		GlobalMonitor.instance.initialize();
	}
	public void onDisable()
	{
		for(Entry<Player, PlaybackContext> ent : mPlayers.entrySet())
			ent.getValue().close();
		
		mPlayers.clear();
		
		GlobalMonitor.instance.shutdown();
		LogFileRegistry.unloadAll();
		CrossReferenceIndex.instance.close();
	}
	
	public synchronized List<String> getPlaybackTargets()
	{
		ArrayList<String> targets = new ArrayList<String>();
		for(Entry<Player, PlaybackContext> ent : mPlayers.entrySet())
		{
			targets.add(ent.getValue().getTargetName(0));
		}
		
		return targets;
	}
	
	public synchronized boolean hasPlayback(Player viewer)
	{
		if(mPlayers.containsKey(viewer))
			return true;
		
		return false;
	}
	public synchronized PlaybackContext createPlayback(Player viewer, String target)
	{
		if(mPlayers.containsKey(viewer))
			return null;
		
		LogFile log = LogFileRegistry.getLogFile(Bukkit.getOfflinePlayer(target));
		if(log == null)
			return null;
		
		// Create the playback
		PlaybackContext playback = new PlaybackContext(log);
		playback.addViewer(viewer);
		mPlayers.put(viewer, playback);
		
		return playback;
	}

	public synchronized boolean removePlayback(Player viewer)
	{
		if(mPlayers.containsKey(viewer))
		{
			int count = 0;
			for(Entry<Player, PlaybackContext> ent : mPlayers.entrySet())
			{
				if(ent.getValue() == mPlayers.get(viewer))
					count++;
			}
			
			mPlayers.get(viewer).removeViewer(viewer);
			if(count <= 1)
			{
				String targetName = mPlayers.get(viewer).getTargetName(0);
				mPlayers.get(viewer).close();

				// Close down the log
				LogFileRegistry.getLogFile(Bukkit.getOfflinePlayer(targetName));
			}
			mPlayers.remove(viewer);
			
			return true;
		}
		return false;
	}
	public PlaybackContext getPlayback(Player viewer)
	{
		return mPlayers.get(viewer);
	}
	
	private void onTick()
	{
		GlobalMonitor.instance.update();
		Searcher.instance.update();
		RollbackManager.instance.update();
		for(Entry<Player, PlaybackContext> playback : mPlayers.entrySet())
		{
			playback.getValue().update();
		}
	}
	
	private HashMap<Player,PlaybackContext> mPlayers = new HashMap<Player,PlaybackContext>();
	public HashSet<PlaybackModeController> PlaybackChatControls = new HashSet<PlaybackModeController>();
	
	private static SpyPlugin sPluginInstance;
	public static SpyPlugin getInstance() { return sPluginInstance; }

	private static Config mConfig;
	public static Config getSettings() { return mConfig; }
	
	private static PriorityExecutor mExecutor = new PriorityExecutor(3);
	public static PriorityExecutor getExecutor() { return mExecutor; }
}
