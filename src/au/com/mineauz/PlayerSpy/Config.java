package au.com.mineauz.PlayerSpy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class Config 
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
	
	protected void onSave()
	{
		mTimeOffsetString = Util.dateDiffToString(timeoffset,true);
		mLogTimeoutString = Util.dateDiffToString(logTimeout,true);
		mTimezoneString = timezone.getID();
	}
	protected void onLoad()
	{
		timeoffset = Util.parseDateDiff(mTimeOffsetString);
		timezone = TimeZone.getTimeZone(mTimezoneString.replace("UTC", "GMT"));
		
		logTimeout = Util.parseDateDiff(mLogTimeoutString);
	}
	
	public boolean load(File file)
	{
		FileConfiguration yml = new YamlConfiguration();
		try
		{
			// Make sure the file exists
			if(!file.exists())
			{
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			
			// Parse the config
			yml.load(file);
			for(Field field : Config.class.getDeclaredFields())
			{
				ConfigField configField = field.getAnnotation(ConfigField.class);
				if(configField == null)
					continue;
				
				String optionName = configField.name();
				if(optionName.isEmpty())
					optionName = field.getName();
				
				String path = (configField.category().isEmpty() ? "" : configField.category() + ".") + optionName;
				if(!yml.contains(path))
				{
					if(field.get(this) == null)
						throw new InvalidConfigurationException(path + " is required to be set! Info:\n" + configField.comment());
				}
				else
				{
					// Parse the value
					
					// Integer
					if(field.getType().equals(Integer.TYPE))
						field.setInt(this, yml.getInt(path));
					
					// Float
					else if(field.getType().equals(Float.TYPE))
						field.setFloat(this, (float)yml.getDouble(path));
					
					// Double
					else if(field.getType().equals(Double.TYPE))
						field.setDouble(this, yml.getDouble(path));
					
					// Long
					else if(field.getType().equals(Long.TYPE))
						field.setLong(this, yml.getLong(path));
					
					// Short
					else if(field.getType().equals(Short.TYPE))
						field.setShort(this, (short)yml.getInt(path));
					
					// Boolean
					else if(field.getType().equals(Boolean.TYPE))
						field.setBoolean(this, yml.getBoolean(path));
					
					// ItemStack
					else if(field.getType().equals(ItemStack.class))
						field.set(this, yml.getItemStack(path));
					
					// String
					else if(field.getType().equals(String.class))
						field.set(this, yml.getString(path));
				}
			}
			
			onLoad();
			
			return true;
		}
		catch( IOException e )
		{
			e.printStackTrace();
			return false;
		}
		catch ( InvalidConfigurationException e )
		{
			e.printStackTrace();
			return false;
		}
		catch ( IllegalArgumentException e )
		{
			e.printStackTrace();
			return false;
		}
		catch ( IllegalAccessException e )
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean save(File file)
	{
		try
		{
			onSave();
			
			YamlConfiguration config = new YamlConfiguration();
			Map<String, String> comments = new HashMap<String, String>();
			
			// Add all the values
			for(Field field : Config.class.getDeclaredFields())
			{
				ConfigField configField = field.getAnnotation(ConfigField.class);
				if(configField == null)
					continue;
				
				String optionName = configField.name();
				if(optionName.isEmpty())
					optionName = field.getName();
				
				String path = (configField.category().isEmpty() ? "" : configField.category() + ".") + optionName;

				// Ensure the secion exists
				if(!configField.category().isEmpty() && !config.contains(configField.category()))
					config.createSection(configField.category());
				
				config.set(path, field.get(this));
				
				// Record the comment
				if(!configField.comment().isEmpty())
					comments.put(path,configField.comment());
			}
			
			String output = config.saveToString();
			
			// Apply comments
			String category = "";
			List<String> lines = new ArrayList<String>(Arrays.asList(output.split("\n")));
			for(int l = 0; l < lines.size(); l++)
			{
				String line = lines.get(l);
				
				if(line.startsWith("#"))
					continue;
				
				if(line.trim().startsWith("-"))
					continue;
				
				if(!line.contains(":"))
					continue;
				
				String path = "";
				line = line.substring(0, line.indexOf(":"));
				
				if(line.startsWith("  "))
					path = category + "." + line.substring(2).trim();
				else
				{
					category = line.trim();
					path = line.trim();
				}
				
				if(comments.containsKey(path))
				{
					String indent = "";
					for(int i = 0; i < line.length(); i++)
					{
						if(line.charAt(i) == ' ')
							indent += " ";
						else
							break;
					}
					
					// Add in the comment lines
					String[] commentLines = comments.get(path).split("\n");
					lines.add(l++, "");
					for(int i = 0; i < commentLines.length; i++)
					{
						commentLines[i] = indent + "# " + commentLines[i];
						lines.add(l++,commentLines[i]);
					}
				}
			}
			output = "";
			for(String line : lines)
				output += line + "\n";
			
			FileWriter writer = new FileWriter(file);
			writer.write(output);
			writer.close();
			return true;
		}
		catch ( IllegalArgumentException e )
		{
			e.printStackTrace();
		}
		catch ( IllegalAccessException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		return false;
	}
}
