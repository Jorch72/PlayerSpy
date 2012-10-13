package au.com.mineauz.PlayerSpy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import au.com.mineauz.PlayerSpy.Upgrading.Upgrader;
import au.com.mineauz.PlayerSpy.commands.*;
import au.com.mineauz.PlayerSpy.monitoring.CrossReferenceIndex;
import au.com.mineauz.PlayerSpy.monitoring.GlobalMonitor;
import au.com.mineauz.PlayerSpy.monitoring.LogFileRegistry;
import au.com.mineauz.PlayerSpy.monitoring.Monitor;
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
		getLogger().setLevel(Level.FINEST);
		getLogger().getParent().getHandlers()[0].setLevel(Level.FINEST);
		//****END DEBUG****
		
		// Load the config
		getSettings().load(new File(getDataFolder(), "config.yml"));
		getSettings().save(new File(getDataFolder(), "config.yml"));
		
		mPersistFile = new File(getDataFolder(), "persist.yml");
		
		try
		{
			if(!mPersistFile.exists())
				mPersistFile.createNewFile();
			
			mPersist = new YamlConfiguration();
			mPersist.load(mPersistFile);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		} 
		catch (InvalidConfigurationException e) 
		{
			e.printStackTrace();
		}
		
		sPluginInstance = this;
		
		Calendar.getInstance().setTimeZone(getSettings().timezone);
		Calendar.getInstance().add(Calendar.MILLISECOND, (int) getSettings().timeoffset);
		
		getCommand("playerspy").setExecutor(new CommandDispatcher());
		
		// Update every tick
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

			@Override
			public void run() 
			{
				onTick();
			}
			
		}, 1L, 1L);

		LogFileRegistry.setLogFileDirectory(getDataFolder());
		if(!CrossReferenceIndex.instance.initialize())
		{
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		Upgrader.run();
		
		//addExistingMonitors();
		GlobalMonitor.instance.initialize();
	}
	public void onDisable()
	{
		for(Entry<Player, PlaybackContext> ent : mPlayers.entrySet())
		{
			ent.getValue().close();

			// Close down the log
			LogFile log = mLogFiles.get(ent.getValue().getTargetName(0).toLowerCase());
		
			log.close(true);
			if(!log.isLoaded())
				mLogFiles.remove(ent.getValue().getTargetName(0).toLowerCase());
		}
		
		mPlayers.clear();
		
		for(Entry<String, Monitor> ent : mMonitors.entrySet())
		{
			
			ent.getValue().stop();

			// Close down the log
			LogFile log = mLogFiles.get(ent.getKey().toLowerCase());
		
			log.close(true);
			if(!log.isLoaded())
				mLogFiles.remove(ent.getValue().getPlayer().toLowerCase());
		}
		
		mMonitors.clear();
		
		GlobalMonitor.instance.shutdown();
		CrossReferenceIndex.instance.close();
		
	}

	private void addExistingMonitors()
	{
		LogUtil.fine("Adding existing monitors");
		if(mPersist.isList("monitors"))
		{
			@SuppressWarnings("unchecked")
			List<String> names = (List<String>) mPersist.getList("monitors");
			
			for(String name : names)
				addMonitor(name);
		}
	}
	@SuppressWarnings("unchecked")
	public synchronized boolean addMonitor(String player)
	{
		if(mMonitors.containsKey(player.toLowerCase()))
			return false;
		
		LogUtil.info("Adding monitor on " + player);
		LogFile log;
		
		if(mLogFiles.containsKey(player.toLowerCase()))
		{
			// It is already open, use it
			log = mLogFiles.get(player.toLowerCase());
			LogUtil.fine("Used already loaded log");
			log.addReference();
		}
		else
		{
			String sanitisedName = player;
			sanitisedName = sanitisedName.replace(':', '_');
			sanitisedName = sanitisedName.replace('/', '_');
			sanitisedName = sanitisedName.replace('\\', '_');
			sanitisedName = sanitisedName.replace('%', '_');
			sanitisedName = sanitisedName.replace('.', '_');
			
			File fileInfo = new File(getDataFolder(),sanitisedName + ".trackdata");
			
			// Open the log file
			if(!fileInfo.exists())
				log = LogFile.create(player, fileInfo.getAbsolutePath());
			else
			{
				log = new LogFile();
				if(!log.load(fileInfo.getAbsolutePath()))
				{
					LogUtil.severe("Unable to open log file for '" + player + "'");
					return false;
				}
			}
			mLogFiles.put(player.toLowerCase(), log);
		}
			
		Monitor monitor = new Monitor(this,player,log);
		mMonitors.put(player.toLowerCase(),monitor);
		
		if(mPersist.isList("monitors"))
		{
			ArrayList<String> names = new ArrayList<String>();
			names.addAll((List<String>) mPersist.getList("monitors"));
			
			if(!names.contains(player.toLowerCase()))
			{
				names.add(player.toLowerCase());
				mPersist.set("monitors", names);
				try {
					mPersist.save(mPersistFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else
		{
			mPersist.set("monitors", Arrays.asList(new String[] { player.toLowerCase() }));
			try {
				mPersist.save(mPersistFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}
	@SuppressWarnings("unchecked")
	public synchronized boolean removeMonitor(String player)
	{
		if(mMonitors.containsKey(player.toLowerCase()))
		{
			mMonitors.get(player.toLowerCase()).stop();
			mLogFiles.get(player.toLowerCase()).closeAsync(true);
			mMonitors.remove(player.toLowerCase());
			
			// Remove the log file if not open
			if(!mLogFiles.get(player.toLowerCase()).isLoaded())
				mLogFiles.remove(player.toLowerCase());
			
			if(mPersist.isList("monitors"))
			{
				ArrayList<String> names = new ArrayList<String>();
				names.addAll((List<String>) mPersist.getList("monitors"));

				if(names.contains(player.toLowerCase()))
				{
					names.remove(player.toLowerCase());
					mPersist.set("monitors", names);
					try {
						mPersist.save(mPersistFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			return true;
		}
		return false;
	}
	public synchronized List<String> getMonitorTargets()
	{
		ArrayList<String> monitors = new ArrayList<String>();
		for(Entry<String, Monitor> ent : mMonitors.entrySet())
		{
			monitors.add(ent.getValue().getPlayer());
		}
		
		return monitors;
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
		
		LogFile log = null;
		
		if(mLogFiles.containsKey(target.toLowerCase()))
		{
			// It is already open, use it
			log = mLogFiles.get(target.toLowerCase());
			LogUtil.fine("Used already loaded log");
		}
		else
		{
			// Open it
			String sanitisedName = target;
			sanitisedName = sanitisedName.replace(':', '_');
			sanitisedName = sanitisedName.replace('/', '_');
			sanitisedName = sanitisedName.replace('\\', '_');
			sanitisedName = sanitisedName.replace('%', '_');
			sanitisedName = sanitisedName.replace('.', '_');
			
			File fileInfo = new File(getDataFolder(),sanitisedName + ".trackdata");
			
			if(!fileInfo.exists())
				return null;
			
			// Attempt to load the log
			log = new LogFile();
			if(!log.load(fileInfo.getAbsolutePath()))
				return null;
			
			mLogFiles.put(target.toLowerCase(), log);
		}
		
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
				LogFile log = mLogFiles.get(targetName.toLowerCase());
			
				log.closeAsync(true);
				if(!log.isLoaded())
					mLogFiles.remove(targetName.toLowerCase());
			}
			mPlayers.remove(viewer);
			
			return true;
		}
		return false;
	}
	public LogFile loadLog(String player)
	{
		LogFile log;
		if(mLogFiles.containsKey(player.toLowerCase()))
		{
			// It is already open, use it
			log = mLogFiles.get(player.toLowerCase());
			LogUtil.fine("Used already loaded log");
		}
		else
		{
			// Open it
			String sanitisedName = player;
			sanitisedName = sanitisedName.replace(':', '_');
			sanitisedName = sanitisedName.replace('/', '_');
			sanitisedName = sanitisedName.replace('\\', '_');
			sanitisedName = sanitisedName.replace('%', '_');
			sanitisedName = sanitisedName.replace('.', '_');
			
			File fileInfo = new File(getDataFolder(),sanitisedName + ".trackdata");
			
			if(!fileInfo.exists())
				return null;
			
			// Attempt to load the log
			log = new LogFile();
			if(!log.load(fileInfo.getAbsolutePath()))
				return null;
			
			mLogFiles.put(player.toLowerCase(), log);
		}
		
		return log;
	}
	public PlaybackContext getPlayback(Player viewer)
	{
		return mPlayers.get(viewer);
	}
	
	public FileConfiguration getPersistantData()
	{
		return mPersist;
	}
	
	private void onTick()
	{
		GlobalMonitor.instance.update();
		Searcher.instance.update();
		for(Entry<Player, PlaybackContext> playback : mPlayers.entrySet())
		{
			playback.getValue().update();
		}
	}
	
	private HashMap<String,Monitor> mMonitors = new HashMap<String,Monitor>();
	private HashMap<Player,PlaybackContext> mPlayers = new HashMap<Player,PlaybackContext>();
	private HashMap<String,LogFile> mLogFiles = new HashMap<String,LogFile>();
	public HashSet<PlaybackModeController> PlaybackChatControls = new HashSet<PlaybackModeController>();
	private FileConfiguration mPersist;
	private File mPersistFile;
	
	private static SpyPlugin sPluginInstance;
	public static SpyPlugin getInstance() { return sPluginInstance; }

	private static Config mConfig = new Config();
	public static Config getSettings() { return mConfig; }
	
	private static PriorityExecutor mExecutor = new PriorityExecutor(3);
	public static PriorityExecutor getExecutor() { return mExecutor; }
}
