package au.com.mineauz.PlayerSpy.Utilities;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.search.Match;

public class Util 
{
	public static long parseDateDiff(String dateDiff)
	{
		if(dateDiff == null)
			return 0;
		
		Pattern dateDiffPattern = Pattern.compile("^\\s*(\\-|\\+)?\\s*(?:([0-9]+)y)?\\s*(?:([0-9]+)mo)?\\s*(?:([0-9]+)w)?\\s*(?:([0-9]+)d)?\\s*(?:([0-9]+)h)?\\s*(?:([0-9]+)m)?\\s*(?:([0-9]+)s)?\\s*$");
		dateDiff = dateDiff.toLowerCase();
		
		Matcher m = dateDiffPattern.matcher(dateDiff);
		
		if(m.matches())
		{
			int years,months,weeks,days,hours,minutes,seconds;
			boolean negative;
			
			if(m.group(1) != null)
				negative = (m.group(1).compareTo("-") == 0);
			else
				negative = false;

			if(m.group(2) != null)
				years = Integer.parseInt(m.group(2));
			else
				years = 0;
			
			if(m.group(3) != null)
				months = Integer.parseInt(m.group(3));
			else
				months = 0;
			
			if(m.group(4) != null)
				weeks = Integer.parseInt(m.group(4));
			else
				weeks = 0;
			
			if(m.group(5) != null)
				days = Integer.parseInt(m.group(5));
			else
				days = 0;
			
			if(m.group(6) != null)
				hours = Integer.parseInt(m.group(6));
			else
				hours = 0;
			
			if(m.group(7) != null)
				minutes = Integer.parseInt(m.group(7));
			else
				minutes = 0;
			
			if(m.group(8) != null)
				seconds = Integer.parseInt(m.group(8));
			else
				seconds = 0;
			
			// Now calculate the time
			long time = 0;
			time += seconds * 1000L;
			time += minutes * 60000L;
			time += hours * 3600000L;
			time += days * 72000000L;
			time += weeks * 504000000L;
			time += months * 2191500000L;
			time += years * 26298000000L;
			
			if(negative)
				time *= -1;
			
			return time;
		}
		
		return 0;
	}
	public static String dateDiffToString(long dateDiff, boolean shortFormat)
	{
		String result = "";
		if(dateDiff < 0)
		{
			result += "-";
			dateDiff *= -1;
		}
		
		int years,months,weeks,days,hours,minutes,seconds;
		if(dateDiff >= 26298000000L)
		{
			years = (int)Math.floor(dateDiff / 26298000000L);
			dateDiff -= years * 26298000000L;
			if(shortFormat)
				result += years + "y";
			else
				result += years + " Year" + (years > 1 ? "s" : "");
		}
		
		if(dateDiff >= 2191500000L)
		{
			months = (int)Math.floor(dateDiff / 2191500000L);
			dateDiff -= months * 2191500000L;
			if(shortFormat)
				result += months + "mo";
			else
				result += months + " Month" + (months > 1 ? "s" : "");
		}
		else
			months = 0;
		
		if(dateDiff >= 504000000L)
		{
			weeks = (int)Math.floor(dateDiff / 504000000L);
			dateDiff -= weeks * 504000000L;
			if(shortFormat)
				result += weeks + "w";
			else
				result += weeks + " Week" + (weeks > 1 ? "s" : "");
		}
		else
			weeks = 0;
		
		if(dateDiff >= 72000000L)
		{
			days = (int)Math.floor(dateDiff / 72000000L);
			dateDiff -= days * 72000000L;
			if(shortFormat)
				result += days + "d";
			else
				result += days + " Day" + (days > 1 ? "s" : "");
		}
		else
			days = 0;
		
		if(dateDiff >= 3600000L)
		{
			hours = (int)Math.floor(dateDiff / 3600000L);
			dateDiff -= hours * 3600000L;
			if(shortFormat)
				result += hours + "h";
			else
				result += hours + " Hour" + (hours > 1 ? "s" : "");
		}
		else
			hours = 0;
		
		if(dateDiff >= 60000L)
		{
			minutes = (int)Math.floor(dateDiff / 60000L);
			dateDiff -= minutes * 60000L;
			if(shortFormat)
				result += minutes + "m";
			else
				result += minutes + " Minute" + (minutes > 1 ? "s" : "");
		}
		else
			minutes = 0;
		
		if(dateDiff >= 0)
		{
			seconds = (int)Math.floor(dateDiff / 1000L);
			dateDiff -= seconds * 1000L;
			if(shortFormat)
				result += seconds + "s";
			else
				result += seconds + " Second" + (seconds > 1 ? "s" : "");
		}
		else
			seconds = 0;
		
		return result;
	}
	public static String dateToString(long date)
	{
		return Utility.formatTime(date, "dd/MM/yy HH:mm:ss");
	}
	public static Match parseDate(String date, long current, long start, long end)
	{
		if(date == null)
			return null;
		
		// Mother of all regular expressions :S
		Pattern datePattern = Pattern.compile("^(?:(?:(\\d{1,2})\\s*/\\s*(\\d{1,2})\\s*(?:/\\s*(\\d{2}|\\d{4}))?\\s+)?(\\d{1,2})\\s*:\\s*(\\d{1,2})\\s*(?::\\s*(\\d{1,2})\\s*)?(am|pm)?|(now|yesterday|current|today|start|end))(?:\\s*(\\-|\\+)\\s*(?:([0-9]+)y)?\\s*(?:([0-9]+)mo)?\\s*(?:([0-9]+)w)?\\s*(?:([0-9]+)d)?\\s*(?:([0-9]+)h)?\\s*(?:([0-9]+)m)?\\s*(?:([0-9]+)s)?\\s*)?");
		date = date.toLowerCase();
		
		Matcher m = datePattern.matcher(date);
		
		if(m.find())
		{
			long time = 0;
			
			int day,month,year;
			int hour,minute,second;
			
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeZone(SpyPlugin.getSettings().timezone);
			
			if(m.group(8) != null)
			{
				if(m.group(8).equals("now"))
				{
					// The date and time of now
					time = Calendar.getInstance().getTimeInMillis();
				}
				else if(m.group(8).equals("current"))
				{
					if(current == 0)
						return null;
					
					time = current;
				}
				else if(m.group(8).equals("today"))
				{
					// Only the date part of now
					GregorianCalendar temp = new GregorianCalendar(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
					temp.setTimeZone(SpyPlugin.getSettings().timezone);
					time = temp.getTimeInMillis();
				}
				else if(m.group(8).equals("yesterday"))
				{
					// today - 1 day
					GregorianCalendar temp = new GregorianCalendar(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
					temp.setTimeZone(SpyPlugin.getSettings().timezone);
					temp.add(Calendar.DAY_OF_MONTH, -1);
					time = temp.getTimeInMillis();
				}
				else if(m.group(8).equals("start"))
				{
					if(start == 0)
						return null;
					
					time = start;
				}
				else if(m.group(8).equals("end"))
				{
					if(end == 0)
						return null;
					
					time = end;
				}
			}
			else
			{
				// Parse the date
				if(m.group(1) != null)
					day = Integer.parseInt(m.group(1));
				else
					day = cal.get(Calendar.DAY_OF_MONTH);
				
				if(m.group(2) != null)
					month = Integer.parseInt(m.group(2));
				else
					month = cal.get(Calendar.MONTH) + 1;
				
				if(m.group(3) != null)
				{
					year = Integer.parseInt(m.group(3));
					if(year < 100)
						year += 2000;
				}
				else
					year = cal.get(Calendar.YEAR);
				
				// Parse the time
				if(m.group(4) != null)
					hour = Integer.parseInt(m.group(4));
				else
					hour = 0;
				
				if(m.group(5) != null)
					minute = Integer.parseInt(m.group(5));
				else
					minute = 0;
				
				if(m.group(6) != null)
					second = Integer.parseInt(m.group(6));
				else
					second = 0;
	
				// Validate the date
				if(month > 12 || month == 0)
					return null;
				
				// Validate day of month
				if(month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12)
				{
					if(day == 0 || day > 31)
						return null;
				}
				else if(month == 2)
				{
					if(day == 0)
						return null;
					if(year % 4 == 0 && year % 400 != 0)
					{
						 if(day > 29)
							 return null;
					}
					else
					{
						if(day > 28)
							 return null;
					}
				}
				else
				{
					if(day == 0 || day > 30)
						return null;
				}
				
				if(minute >= 60)
					return null;
				
				if(second >= 60)
					return null;
				
				// Validate time
				if(m.group(7) != null)
				{
					if(hour > 12 || hour == 0)
						return null;
					
					if(m.group(7).equals("pm"))
					{
						hour += 12;
						if(hour == 24)
							hour = 0;
					}
				}
				else
				{
					if(hour >= 24)
						return null;
				}
				
				
				//time = second * 1000 + minute * 60000 + hour * 360000 + day * 86400000 +
				cal.setTimeZone(SpyPlugin.getSettings().timezone);
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, month-1);
				cal.set(Calendar.DAY_OF_MONTH, day);
				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, minute);
				cal.set(Calendar.SECOND, second);
				cal.set(Calendar.MILLISECOND, 0);

				time = cal.getTimeInMillis();
			}
			
			// Do modification to it
			{
				int years,months,weeks,days,hours,minutes,seconds;
				boolean negative;
				
				if(m.group(9) != null)
					negative = (m.group(9).compareTo("-") == 0);
				else
					negative = false;

				if(m.group(10) != null)
					years = Integer.parseInt(m.group(10));
				else
					years = 0;
				
				if(m.group(11) != null)
					months = Integer.parseInt(m.group(11));
				else
					months = 0;
				
				if(m.group(12) != null)
					weeks = Integer.parseInt(m.group(12));
				else
					weeks = 0;
				
				if(m.group(13) != null)
					days = Integer.parseInt(m.group(13));
				else
					days = 0;
				
				if(m.group(14) != null)
					hours = Integer.parseInt(m.group(14));
				else
					hours = 0;
				
				if(m.group(15) != null)
					minutes = Integer.parseInt(m.group(15));
				else
					minutes = 0;
				
				if(m.group(16) != null)
					seconds = Integer.parseInt(m.group(16));
				else
					seconds = 0;
				
				// Now calculate the time
				long temp = 0;
				temp += seconds * 1000L;
				temp += minutes * 60000L;
				temp += hours * 3600000L;
				temp += days * 72000000L;
				temp += weeks * 504000000L;
				temp += months * 2191500000L;
				temp += years * 26298000000L;
				
				if(negative)
					temp *= -1;
				
				time += temp;
			}
			
			return new Match(0, m.end(), time, null);
		}
		
		return null;
	}

	public static EntityType parseEntity(String entity)
	{
		// Try to parse it though that
		EntityType type = EntityType.fromName(entity);
		
		if(type != null)
			return type;

		// Some are mispelt or use strange names or the may be common alternet names for them
		entity = entity.toLowerCase();
		if(entity.equals("zombiepigman"))
			return EntityType.PIG_ZOMBIE;
		
		if(entity.equals("magmacube"))
			return EntityType.MAGMA_CUBE;
		
		if(entity.equals("mooshroom"))
			return EntityType.MUSHROOM_COW;
		
		if(entity.equals("ocelot"))
			return EntityType.OCELOT;
		
		if(entity.equals("cat"))
			return EntityType.OCELOT;
		
		if(entity.equals("irongolem"))
			return EntityType.IRON_GOLEM;

		if(entity.equals("snowgolem"))
			return EntityType.SNOWMAN;
		
		if(entity.equals("dog"))
			return EntityType.WOLF;
		
		if(entity.equals("player"))
			return EntityType.PLAYER;

		
		return null;
	}
	
	public static Material parseBlock(String block)
	{
		// Try to parse it as an int
		try
		{
			int id = Integer.parseInt(block);
			Material mat = Material.getMaterial(id);
			if(mat == null)
				return null;
			
			if(mat.isBlock())
				return mat;
			else
				return null;
		}
		catch (NumberFormatException e)
		{
		}
		
		// Not an id
		// try to get it by name
		Material mat = Material.getMaterial(block.toUpperCase());
		if(mat != null)
		{
			if(mat.isBlock())
				return mat;
		}
		
		// Try alternet names
		block = block.toLowerCase();
		
		if(block.equals("smoothstone"))
			return Material.STONE;
		if(block.equals("grassblock"))
			return Material.GRASS;
		if(block.equals("cobble"))
			return Material.COBBLESTONE;
		if(block.equals("planks"))
			return Material.WOOD;
		if(block.equals("gold") || block.equals("goldore"))
			return Material.GOLD_ORE;
		if(block.equals("iron") || block.equals("ironore"))
			return Material.IRON_ORE;
		if(block.equals("coal") || block.equals("coalore"))
			return Material.COAL_ORE;
		if(block.equals("lapis") || block.equals("lapisore") || block.equals("lapislazuli"))
			return Material.LAPIS_ORE;
		if(block.equals("emerald") || block.equals("emeraldore"))
			return Material.EMERALD_ORE;
		if(block.equals("emeraldblock"))
			return Material.EMERALD_BLOCK;
		if(block.equals("noteblock"))
			return Material.NOTE_BLOCK;
		if(block.equals("bed"))
			return Material.BED_BLOCK;
		if(block.equals("poweredrail"))
			return Material.POWERED_RAIL;
		if(block.equals("detectorrail"))
			return Material.DETECTOR_RAIL;
		if(block.equals("sickypison"))
			return Material.PISTON_STICKY_BASE;
		if(block.equals("longgrass"))
			return Material.LONG_GRASS;
		if(block.equals("deadbush") || block.equals("desertbush"))
			return Material.DEAD_BUSH;
		if(block.equals("piston"))
			return Material.PISTON_BASE;
		if(block.equals("yellowflower") || block.equals("dandilion"))
			return Material.YELLOW_FLOWER;
		if(block.equals("rose") || block.equals("redrose"))
			return Material.RED_ROSE;
		if(block.equals("brownmushroom"))
			return Material.BROWN_MUSHROOM;
		if(block.equals("redmushroom"))
			return Material.RED_MUSHROOM;
		if(block.equals("goldblock"))
			return Material.GOLD_BLOCK;
		if(block.equals("ironblock"))
			return Material.IRON_BLOCK;
		if(block.equals("doubleslab"))
			return Material.DOUBLE_STEP;
		if(block.equals("slab") || block.equals("halfslab"))
			return Material.STEP;
		if(block.equals("mossycobblestone") || block.equals("mossycobble"))
			return Material.MOSSY_COBBLESTONE;
		if(block.equals("mobspawner") || block.equals("spawner"))
			return Material.MOB_SPAWNER;
		if(block.equals("woodenstairs") || block.equals("woordstairs"))
			return Material.WOOD_STAIRS;
		if(block.equals("redstonewire") || block.equals("redstonedust") || block.equals("wire"))
			return Material.REDSTONE_WIRE;
		if(block.equals("diamond") || block.equals("diamondore"))
			return Material.DIAMOND_ORE;
		if(block.equals("diamondblock"))
			return Material.DIAMOND_BLOCK;
		if(block.equals("farm"))
			return Material.CROPS;
		if(block.equals("sign"))
			return Material.SIGN_POST;
		if(block.equals("stoneplate") || block.equals("stonepressureplate"))
			return Material.STONE_PLATE;
		if(block.equals("irondoor"))
			return Material.IRON_DOOR_BLOCK;
		if(block.equals("woodplate") || block.equals("woodenplate") || block.equals("woodpressureplate") || block.equals("woodenpressureplate"))
			return Material.WOOD_PLATE;
		if(block.equals("redstone") || block.equals("redstoneore"))
			return Material.REDSTONE_ORE;
		if(block.equals("redstonetorch"))
			return Material.REDSTONE_TORCH_ON;
		if(block.equals("button") || block.equals("stonebutton"))
			return Material.STONE_BUTTON;
		if(block.equals("snowblock"))
			return Material.SNOW_BLOCK;
		if(block.equals("reeds") || block.equals("sugarcane") || block.equals("cane"))
			return Material.SUGAR_CANE_BLOCK;
		if(block.equals("soulsand"))
			return Material.SOUL_SAND;
		if(block.equals("jackolantern"))
			return Material.JACK_O_LANTERN;
		if(block.equals("cake"))
			return Material.CAKE_BLOCK;
		if(block.equals("repeater") || block.equals("diode"))
			return Material.DIODE_BLOCK_ON;
		if(block.equals("lockedchest"))
			return Material.LOCKED_CHEST;
		if(block.equals("trapdoor"))
			return Material.TRAP_DOOR;
		if(block.equals("giantmushroom") || block.equals("hugemushroom"))
			return Material.HUGE_MUSHROOM_1;
		if(block.equals("ironfence"))
			return Material.IRON_FENCE;
		if(block.equals("glasspane") || block.equals("thinglass"))
			return Material.THIN_GLASS;
		if(block.equals("melon") || block.equals("melons"))
			return Material.MELON_BLOCK;
		if(block.equals("pumpkinstem") || block.equals("pumpkinplant"))
			return Material.PUMPKIN_STEM;
		if(block.equals("melonstem") || block.equals("melonplant"))
			return Material.MELON_STEM;
		if(block.equals("gate") || block.equals("fencegate"))
			return Material.FENCE_GATE;
		if(block.equals("brickstairs"))
			return Material.BRICK_STAIRS;
		if(block.equals("stonebrickstairs"))
			return Material.SMOOTH_STAIRS;
		if(block.equals("mycel") || block.equals("mycelium"))
			return Material.MYCEL;
		if(block.equals("lily") || block.equals("waterlily"))
			return Material.WATER_LILY;
		if(block.equals("netherbrick"))
			return Material.NETHER_BRICK;
		if(block.equals("netherfence"))
			return Material.NETHER_FENCE;
		if(block.equals("netherbrickstairs") || block.equals("netherstairs"))
			return Material.NETHER_BRICK_STAIRS;
		if(block.equals("netherwart") || block.equals("wart") || block.equals("netherwarts") || block.equals("warts"))
			return Material.NETHER_WARTS;
		if(block.equals("enchantmenttable") || block.equals("enchanttable"))
			return Material.ENCHANTMENT_TABLE;
		if(block.equals("brewingstand"))
			return Material.BREWING_STAND;
		if(block.equals("enderportalframe") || block.equals("endportalframe") || block.equals("portalframe"))
			return Material.ENDER_PORTAL_FRAME;
		if(block.equals("endstone") || block.equals("enderstone") || block.equals("whitestone"))
			return Material.ENDER_STONE;
		if(block.equals("dragonegg"))
			return Material.DRAGON_EGG;
		if(block.equals("lamp") || block.equals("redstonelamp"))
			return Material.REDSTONE_LAMP_ON;
		if(block.equals("enderchest"))
			return Material.ENDER_CHEST;
		if(block.equals("tripwirehook"))
			return Material.TRIPWIRE_HOOK;
		if(block.equals("cocoapod"))
			return Material.COCOA;
		
		return null;
	}
	
	public static Material parseItem(String item)
	{
		// Try to parse it as an int
		try
		{
			int id = Integer.parseInt(item);
			Material mat = Material.getMaterial(id);
				return mat;
		}
		catch (NumberFormatException e)
		{
		}
		
		// Not an id
		// try to get it by name
		Material mat = Material.getMaterial(item.toUpperCase());
		if(mat != null)
			return mat;
		
		// Try alternet names
		item = item.toLowerCase();
		
		if(item.equals("smoothstone"))
			return Material.STONE;
		if(item.equals("grassitem"))
			return Material.GRASS;
		if(item.equals("cobble"))
			return Material.COBBLESTONE;
		if(item.equals("planks"))
			return Material.WOOD;
		if(item.equals("goldore"))
			return Material.GOLD_ORE;
		if(item.equals("ironore"))
			return Material.IRON_ORE;
		if(item.equals("coalore"))
			return Material.COAL_ORE;
		if(item.equals("lapisore"))
			return Material.LAPIS_ORE;
		if(item.equals("emeraldore"))
			return Material.EMERALD_ORE;
		if(item.equals("emeraldblock"))
			return Material.EMERALD_BLOCK;
		if(item.equals("noteblock"))
			return Material.NOTE_BLOCK;
		if(item.equals("bed"))
			return Material.BED;
		if(item.equals("poweredrail"))
			return Material.POWERED_RAIL;
		if(item.equals("detectorrail"))
			return Material.DETECTOR_RAIL;
		if(item.equals("sickypison"))
			return Material.PISTON_STICKY_BASE;
		if(item.equals("longgrass"))
			return Material.LONG_GRASS;
		if(item.equals("deadbush") || item.equals("desertbush"))
			return Material.DEAD_BUSH;
		if(item.equals("piston"))
			return Material.PISTON_BASE;
		if(item.equals("yellowflower") || item.equals("dandilion"))
			return Material.YELLOW_FLOWER;
		if(item.equals("rose") || item.equals("redrose"))
			return Material.RED_ROSE;
		if(item.equals("brownmushroom"))
			return Material.BROWN_MUSHROOM;
		if(item.equals("redmushroom"))
			return Material.RED_MUSHROOM;
		if(item.equals("goldblock"))
			return Material.GOLD_BLOCK;
		if(item.equals("ironblock"))
			return Material.IRON_BLOCK;
		if(item.equals("doubleslab"))
			return Material.DOUBLE_STEP;
		if(item.equals("slab") || item.equals("halfslab"))
			return Material.STEP;
		if(item.equals("mossycobblestone") || item.equals("mossycobble"))
			return Material.MOSSY_COBBLESTONE;
		if(item.equals("mobspawner") || item.equals("spawner"))
			return Material.MOB_SPAWNER;
		if(item.equals("woodenstairs") || item.equals("woordstairs"))
			return Material.WOOD_STAIRS;
		if(item.equals("redstonewire") || item.equals("redstonedust") || item.equals("wire"))
			return Material.REDSTONE_WIRE;
		if(item.equals("diamondore"))
			return Material.DIAMOND_ORE;
		if(item.equals("diamondblock"))
			return Material.DIAMOND_BLOCK;
		if(item.equals("farm"))
			return Material.CROPS;
		if(item.equals("sign"))
			return Material.SIGN;
		if(item.equals("stoneplate") || item.equals("stonepressureplate"))
			return Material.STONE_PLATE;
		if(item.equals("irondoor"))
			return Material.IRON_DOOR;
		if(item.equals("woodplate") || item.equals("woodenplate") || item.equals("woodpressureplate") || item.equals("woodenpressureplate"))
			return Material.WOOD_PLATE;
		if(item.equals("redstoneore"))
			return Material.REDSTONE_ORE;
		if(item.equals("redstonetorch"))
			return Material.REDSTONE_TORCH_ON;
		if(item.equals("button") || item.equals("stonebutton"))
			return Material.STONE_BUTTON;
		if(item.equals("snowblock"))
			return Material.SNOW_BLOCK;
		if(item.equals("reeds") || item.equals("sugarcane") || item.equals("cane"))
			return Material.SUGAR_CANE;
		if(item.equals("soulsand"))
			return Material.SOUL_SAND;
		if(item.equals("jackolantern"))
			return Material.JACK_O_LANTERN;
		if(item.equals("cake"))
			return Material.CAKE;
		if(item.equals("repeater"))
			return Material.DIODE;
		if(item.equals("lockedchest"))
			return Material.LOCKED_CHEST;
		if(item.equals("trapdoor"))
			return Material.TRAP_DOOR;
		if(item.equals("giantmushroom") || item.equals("hugemushroom"))
			return Material.HUGE_MUSHROOM_1;
		if(item.equals("ironfence"))
			return Material.IRON_FENCE;
		if(item.equals("glasspane") || item.equals("thinglass"))
			return Material.THIN_GLASS;
		if(item.equals("melonblock"))
			return Material.MELON_BLOCK;
		if(item.equals("pumpkinstem") || item.equals("pumpkinplant"))
			return Material.PUMPKIN_STEM;
		if(item.equals("melonstem") || item.equals("melonplant"))
			return Material.MELON_STEM;
		if(item.equals("gate") || item.equals("fencegate"))
			return Material.FENCE_GATE;
		if(item.equals("brickstairs"))
			return Material.BRICK_STAIRS;
		if(item.equals("stonebrickstairs"))
			return Material.SMOOTH_STAIRS;
		if(item.equals("mycel") || item.equals("mycelium"))
			return Material.MYCEL;
		if(item.equals("lily") || item.equals("waterlily"))
			return Material.WATER_LILY;
		if(item.equals("netherbrick"))
			return Material.NETHER_BRICK;
		if(item.equals("netherfence"))
			return Material.NETHER_FENCE;
		if(item.equals("netherbrickstairs") || item.equals("netherstairs"))
			return Material.NETHER_BRICK_STAIRS;
		if(item.equals("netherwart") || item.equals("wart") || item.equals("netherwarts") || item.equals("warts"))
			return Material.NETHER_STALK;
		if(item.equals("enchantmenttable") || item.equals("enchanttable"))
			return Material.ENCHANTMENT_TABLE;
		if(item.equals("brewingstand"))
			return Material.BREWING_STAND_ITEM;
		if(item.equals("enderportalframe") || item.equals("endportalframe") || item.equals("portalframe"))
			return Material.ENDER_PORTAL_FRAME;
		if(item.equals("endstone") || item.equals("enderstone") || item.equals("whitestone"))
			return Material.ENDER_STONE;
		if(item.equals("dragonegg"))
			return Material.DRAGON_EGG;
		if(item.equals("lamp") || item.equals("redstonelamp"))
			return Material.REDSTONE_LAMP_ON;
		if(item.equals("enderchest"))
			return Material.ENDER_CHEST;
		
		for(Material material : Material.values())
		{
			String matName = material.name();
			if(matName == null)
				continue;
			
			matName = matName.toLowerCase().replace("_", "");
			if(item.equals(matName))
				return material;
		}
		
		if(item.equals("iron"))
			return Material.IRON_INGOT;
		if(item.equals("gold"))
			return Material.GOLD_INGOT;
		if(item.equals("soup"))
			return Material.MUSHROOM_SOUP;
		if(item.equals("gunpowder"))
			return Material.SULPHUR;
		if(item.equals("cookedpork"))
			return Material.GRILLED_PORK;
		if(item.equals("woodendoor"))
			return Material.WOOD_DOOR;
		if(item.equals("brick"))
			return Material.CLAY_BRICK;
		if(item.equals("glisteningmelon"))
			return Material.SPECKLED_MELON;
		if(item.equals("spawnegg"))
			return Material.MONSTER_EGG;
		if(item.equals("bottleofenchanting") || item.equals("bottleoenchanting"))
			return Material.EXP_BOTTLE;
		
		return null;
	}
}
