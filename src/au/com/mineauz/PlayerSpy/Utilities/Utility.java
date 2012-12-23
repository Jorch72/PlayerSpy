package au.com.mineauz.PlayerSpy.Utilities;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_4_6.CraftWorld;
import org.bukkit.craftbukkit.v1_4_6.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;

import au.com.mineauz.PlayerSpy.InventorySlot;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.UpdateInventoryRecord;

import net.minecraft.server.v1_4_6.*;

public class Utility 
{
	public static ItemStack convertToNative(org.bukkit.inventory.ItemStack item)
	{
		if(item == null)
			return null;
		if(item instanceof CraftItemStack)
			return CraftItemStack.asNMSCopy(item);
		else
		{
			ItemStack nativeStack = new ItemStack(item.getTypeId(), item.getAmount(), item.getDurability());
			
			if(item.getEnchantments().size() != 0)
			{
				if(nativeStack.tag == null)
					nativeStack.tag = new NBTTagCompound();
			
				NBTTagList list = new NBTTagList();
				// Add enchants
				for(Entry<Enchantment, Integer> ench : item.getEnchantments().entrySet())
				{
					NBTTagCompound tag = new NBTTagCompound();
					tag.setShort("id", (short)ench.getKey().getId());
					tag.setShort("lvl", (short)(int)ench.getValue());
					list.add(tag);
				}
				
				nativeStack.tag.set("ench", list);
			}
			
			return nativeStack;
		}
	}
	
	public static Packet5EntityEquipment makeEntityEquipmentPacket(int entity, int slot, org.bukkit.inventory.ItemStack item)
	{
		return new Packet5EntityEquipment(entity, slot, convertToNative(item));
	}
	
	public static EntityItem makeEntityItem(Location location, org.bukkit.inventory.ItemStack item)
	{
		assert location != null : "Cannot set a null location";
		assert item != null : "Cannot set a null item";
		EntityItem entityItem = new EntityItem(((CraftWorld)location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ(), convertToNative(item));
		
		entityItem.motX = 0;
		entityItem.motY = 0;
		entityItem.motZ = 0;
		
		return entityItem;
	}
	public static Packet23VehicleSpawn makeItemSpawnPacket(Location location, org.bukkit.inventory.ItemStack item)
	{
		return new Packet23VehicleSpawn(makeEntityItem(location, item), 2, 1);
	}
	
	public static void setEntityPosition(Entity ent, Location loc)
	{
		assert loc != null : "Cannot set a null location";
		
		ent.world = ((CraftWorld)loc.getWorld()).getHandle();
		ent.lastX = ent.locX;
		ent.lastY = ent.locY;
		ent.lastZ = ent.locZ;
		
		ent.locX = loc.getX();
		ent.locY = loc.getY();
		ent.locZ = loc.getZ();
		
		ent.lastYaw = ent.yaw;
		ent.yaw = loc.getYaw();
		ent.lastPitch = ent.pitch;
		ent.pitch = loc.getPitch();

		ent.positionChanged = true;
	}
	
	public static void setEntityHeadLook(EntityLiving ent, float yaw, float pitch)
	{
		// Last cam yaw
		ent.aA = ent.az;
		// Cam yaw
		ent.az = yaw;
		
		// Last camera pitch
		ent.ba = ent.bb;
		// cam pitch
		ent.bb = pitch;
	}
	
	public static org.bukkit.inventory.ItemStack splitStack(org.bukkit.inventory.ItemStack stack, int amount)
	{
		org.bukkit.inventory.ItemStack result = new org.bukkit.inventory.ItemStack(stack.getTypeId(), amount, stack.getDurability());
		
		if(stack.getEnchantments() != null)
			result.addEnchantments(stack.getEnchantments());
		
		stack.setAmount(stack.getAmount() - amount);
		return result;
	}
	public static UpdateInventoryRecord createUpdateRecordFor(int slot, boolean leftButton, boolean shift, org.bukkit.inventory.ItemStack cursor, org.bukkit.inventory.ItemStack existing)
	{
		org.bukkit.inventory.ItemStack result = null;
		boolean changed = false;
		if(existing != null)
		{
			if(existing.getTypeId() == 0)
				existing = null;
			else
				existing = existing.clone();
		}
		
		if(cursor != null)
		{
			if(cursor.getTypeId() == 0)
				cursor = null;
			else
				cursor = cursor.clone();
		}
		
		// Place into slot
		if(existing == null)
		{
			if(cursor != null)
			{
				int placeCount = (leftButton ? cursor.getAmount() : 1);
				
				// Check that it is valid
				if((slot == 36 && (cursor.getType() == Material.LEATHER_BOOTS || cursor.getType() == Material.IRON_BOOTS || cursor.getType() == Material.CHAINMAIL_BOOTS || cursor.getType() == Material.GOLD_BOOTS || cursor.getType() == Material.DIAMOND_BOOTS)) ||
					(slot == 37 && (cursor.getType() == Material.LEATHER_LEGGINGS || cursor.getType() == Material.IRON_LEGGINGS || cursor.getType() == Material.CHAINMAIL_LEGGINGS || cursor.getType() == Material.GOLD_LEGGINGS || cursor.getType() == Material.DIAMOND_LEGGINGS)) ||
					(slot == 38 && (cursor.getType() == Material.LEATHER_CHESTPLATE || cursor.getType() == Material.IRON_CHESTPLATE || cursor.getType() == Material.CHAINMAIL_CHESTPLATE || cursor.getType() == Material.GOLD_CHESTPLATE || cursor.getType() == Material.DIAMOND_CHESTPLATE)) ||
					(slot == 39 && (cursor.getType() == Material.LEATHER_HELMET || cursor.getType() == Material.IRON_HELMET || cursor.getType() == Material.CHAINMAIL_HELMET || cursor.getType() == Material.GOLD_HELMET || cursor.getType() == Material.DIAMOND_HELMET)) ||
					slot < 36)
				{
					if(cursor.getAmount() >= placeCount)
					{
						result = splitStack(cursor, placeCount);
						changed = true;
					}
				}
			}
		}
		// Pickup from slot
		else if(cursor == null)
		{
			int amount = (leftButton ? existing.getAmount() : (existing.getAmount() + 1) / 2);
			splitStack(existing,amount);
			
			if(existing.getAmount() == 0)
				existing = null;
			
			changed = true;
			result = existing;
		}
		// Add to slot if valid
		else if((slot == 36 && (cursor.getType() == Material.LEATHER_BOOTS || cursor.getType() == Material.IRON_BOOTS || cursor.getType() == Material.CHAINMAIL_BOOTS || cursor.getType() == Material.GOLD_BOOTS || cursor.getType() == Material.DIAMOND_BOOTS)) ||
			(slot == 37 && (cursor.getType() == Material.LEATHER_LEGGINGS || cursor.getType() == Material.IRON_LEGGINGS || cursor.getType() == Material.CHAINMAIL_LEGGINGS || cursor.getType() == Material.GOLD_LEGGINGS || cursor.getType() == Material.DIAMOND_LEGGINGS)) ||
			(slot == 38 && (cursor.getType() == Material.LEATHER_CHESTPLATE || cursor.getType() == Material.IRON_CHESTPLATE || cursor.getType() == Material.CHAINMAIL_CHESTPLATE || cursor.getType() == Material.GOLD_CHESTPLATE || cursor.getType() == Material.DIAMOND_CHESTPLATE)) ||
			(slot == 39 && (cursor.getType() == Material.LEATHER_HELMET || cursor.getType() == Material.IRON_HELMET || cursor.getType() == Material.CHAINMAIL_HELMET || cursor.getType() == Material.GOLD_HELMET || cursor.getType() == Material.DIAMOND_HELMET)) ||
			slot < 36)
		{
			// They are the same type
			if(existing.getData().equals(cursor.getData()))
			{
				int amount = (leftButton ? cursor.getAmount() : 1);
				if(amount > cursor.getMaxStackSize() - existing.getAmount())
				{
					amount = cursor.getMaxStackSize() - existing.getAmount();
				}
				
				existing.setAmount(existing.getAmount() + amount);
				
				changed = true;
				result = existing;
			}
			// They are different types
			else
			{
				existing = cursor;
				result = existing;
				changed = true;
			}
		}
		
		if(changed)
		{
			ArrayList<InventorySlot> updates = new ArrayList<InventorySlot>();
			updates.add(new InventorySlot(result, slot));
			return new UpdateInventoryRecord(updates);
		}
		return null;
	}
	
	public static Location getLocation(Entity ent)
	{
		Location loc = new Location(ent.world.getWorld(), ent.locX, ent.locY, ent.locZ);
		return loc;
	}
	
	public static boolean areEqualIgnoreAmount(org.bukkit.inventory.ItemStack a, org.bukkit.inventory.ItemStack b)
	{
		if((a == null && b != null) || (a != null && b == null))
			return false;
		
		if(a == null && b == null)
			return true;
		
		return (a.getType() == b.getType() && a.getDurability() == b.getDurability() && a.getEnchantments().equals(b.getEnchantments()));
	}
	
	public static org.bukkit.inventory.ItemStack getStackOrNull(org.bukkit.inventory.ItemStack stack)
	{
		if(stack.getTypeId() == 0)
			return null;
		
		return stack;
	}
	
	public static String locationToString(Location loc)
	{
		return String.format("(%.2f, %.2f, %.2f, %s)", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
	}
	public static String locationToStringShort(Location loc)
	{
		return String.format("(%.0f,%.0f,%.0f,%s)", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
	}
	public static String formatItemName( org.bukkit.inventory.ItemStack myItem )
    {
		ItemStack nativeStack = convertToNative(myItem);
		
		if(nativeStack.getItem() instanceof ItemPotion)
		{
			if (nativeStack.getData() == 0)
	        {
	            return StringTranslator.translateString("item.emptyPotion.name").trim();
	        }
	        else
	        {
	            String prefix = "";

	            if (ItemPotion.g(nativeStack.getData()))
	            {
	                prefix = StringTranslator.translateString("potion.prefix.grenade").trim() + " ";
	            }

	            List<?> effects = Item.POTION.g(nativeStack);
	            String name;

	            if (effects != null && !effects.isEmpty())
	            {
	                name = ((MobEffect)effects.get(0)).f();
	                name += ".postfix";
	                return prefix + StringTranslator.translateString(name).trim();
	            }
	            else
	            {
	            	
	            	try
	            	{
	            		Field field = PotionBrewer.class.getDeclaredField("appearances");
	            		field.setAccessible(true);
	            		String[] potionPrefixes = (String[])field.get(null);
	            		
	            		int damage = nativeStack.getData();
	            		int index = (PotionBrewer.a(damage,5) ? 16 : 0) | (PotionBrewer.a(damage,4) ? 8 : 0) | (PotionBrewer.a(damage,3) ? 4 : 0) | (PotionBrewer.a(damage,2) ? 2 : 0) | (PotionBrewer.a(damage,1) ? 1 : 0);
	            		name = StringTranslator.translateString(potionPrefixes[index]) + " " + StringTranslator.translateName(nativeStack.getItem().d(nativeStack));
	            		return name;
	            	}
	            	catch(NoSuchFieldException e)
	            	{
	            		e.printStackTrace();
	            	}
	            	catch(IllegalArgumentException e)
	            	{
	            		e.printStackTrace();
	            	}
					catch ( IllegalAccessException e )
					{
						e.printStackTrace();
					}
	            	
	            	
	            }
	        }
		}
		
		String result = StringTranslator.translateName(nativeStack.getItem().d(nativeStack));
		if(result.trim().isEmpty())
		{
	        String name = new StringBuilder().append( Character.toUpperCase(myItem.getType().name().charAt(0)) )
	                .append( myItem.getType().name().substring(1).toLowerCase() ).toString();
	        name = name.replace('_', ' ');
	        for ( int i = 0; i < name.length(); i++ )
	        {
	            StringBuilder sb = new StringBuilder();
	            if ( name.charAt( i ) == ' ' && i < name.length() - 1 )
	            {
	                name = sb.append( name.substring(0, i + 1) ).append( Character.toUpperCase(name.charAt( i + 1 ) ) ).append(name.substring( i + 2 ) ).toString();
	            }
	        }
	        return name;
		}
		else
			return result;
    }
	
	public static String formatName(String playerName, String cause)
	{
		if(cause == null)
			return playerName;
		else
			return playerName + ">" + cause;
	}
	
	public static long getDatePortion(long timeDate)
	{
		long time = timeDate;
		time = (time / 86400000) * 86400000;
		return time;
	}
	public static long getTimePortion(long timeDate)
	{
		return timeDate - getDatePortion(timeDate);
	}
	public static String formatTime(long time, String format)
	{
		SimpleDateFormat fmt = new SimpleDateFormat(format);
		fmt.setTimeZone(SpyPlugin.getSettings().timezone);
		return fmt.format(new Date(time));
	}
	public static org.bukkit.inventory.ItemStack matchName(String input)
	{
		String keyName = null;
		HashMap<String,String> table = StringTranslator.getStringTable();
		
		input = input.replaceAll("_", " ");
		
		for(Entry<String,String> entry : table.entrySet())
		{
			if(entry.getValue().equalsIgnoreCase(input))
			{
				keyName = entry.getKey();
				if(keyName.contains(".name"))
					break;
			}
		}
		
		if(keyName == null)
			return null;

		if(!keyName.contains(".name"))
			return null;
		
		keyName = keyName.substring(0, keyName.indexOf(".name"));
		// Now search items for a key match
		for(Item item : Item.byId)
		{
			if(item == null)
				continue;
			
			if(item.l()) // Item has subtypes
			{
				// Search the first 16 ids
				for(int i = 0; i < 16; i++)
				{
					String name = item.d(new ItemStack(item,1,i));
					if(keyName.equals(name))
						return new org.bukkit.inventory.ItemStack(item.id,1,(short)i);
				}
			}
			else if(keyName.equals(item.getName()))
			{
				return new org.bukkit.inventory.ItemStack(item.id, 1, (short)0);
			}
		}
		
		return null;
	}
	/**
	 * Gets the number of bytes that will be written to a stream using writeUTF()
	 * @param string
	 * @return
	 */
	public static int getUTFLength(String string)
	{
		int length = 2;
		for(int i = 0; i < string.length(); i++)
		{
			if(string.charAt(i) >= 0x01 && string.charAt(i) <= 0x7f)
				length++;
			else if(string.charAt(i) == 0x00 || (string.charAt(i) >= 0x80 && string.charAt(i) <= 0x7ff))
				length += 2;
			else if(string.charAt(i) >= 0x800 && string.charAt(i) <= 0xffff)
				length += 3;
		}
		
		return length;
	}
}
