package au.com.mineauz.PlayerSpy.storage;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.meta.*;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;
import au.com.mineauz.PlayerSpy.Utilities.ReflectionHelper;
import au.com.mineauz.PlayerSpy.wrappers.nbt.*;

public class StoredItemMeta
{
	private ItemMeta mMeta;
	private static Class<?> itemMetaDeserializerClass;
	
	static
	{
		try
		{
			itemMetaDeserializerClass = ReflectionHelper.forName("org.bukkit.craftbukkit.*.inventory.CraftMetaItem$SerializableMeta");
		}
		catch(ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	
	public StoredItemMeta(ItemMeta meta)
	{
		mMeta = meta;
	}
	public StoredItemMeta()
	{
		
	}
	
	public ItemMeta getMeta()
	{
		return mMeta;
	}
	
	private Object makeObjectFor(NBTBase tag)
	{
		Object data = null;
		
		if(tag instanceof NBTTagByte)
			data = ((NBTTagByte)tag).getData();
		else if(tag instanceof NBTTagShort)
			data = ((NBTTagShort)tag).getData();
		else if(tag instanceof NBTTagInt)
			data = ((NBTTagInt)tag).getData();
		else if(tag instanceof NBTTagLong)
			data = ((NBTTagLong)tag).getData();
		
		else if(tag instanceof NBTTagString)
			data = ((NBTTagString)tag).getData();
		
		else if(tag instanceof NBTTagFloat)
			data = ((NBTTagFloat)tag).getData();
		else if(tag instanceof NBTTagDouble)
			data = ((NBTTagDouble)tag).getData();
		
		else if(tag instanceof NBTTagList)
		{
			List<Object> list = new ArrayList<Object>();
			for(int i = 0; i < ((NBTTagList)tag).size(); ++i)
			{
				NBTBase subTag = ((NBTTagList)tag).get(i);
				
				list.add(makeObjectFor(subTag));
			}
			
			data = list;
		}
		
		else if(tag instanceof NBTTagCompound)
		{
			String type = ((NBTTagCompound)tag).getString("type");
			NBTBase value = ((NBTTagCompound)tag).get("value");
			
			if(type.equals("color"))
			{
				data = Color.fromRGB(((NBTTagInt)value).getData());
			}
			else if(type.equals("map"))
			{
				Map<String, Object> map = new HashMap<String, Object>();
				
				Collection<NBTBase> tags = (Collection<NBTBase>)((NBTTagCompound)value).getTags();
				
				for(NBTBase subTag : tags)
				{
					map.put(subTag.getName(), makeObjectFor(subTag));
				}
				
				data = map;
			}
			else if(type.equals("firework"))
			{
				boolean trail = ((NBTTagCompound)tag).getByte("hasTrail") == 1;
				boolean flicker = ((NBTTagCompound)tag).getByte("hasFlicker") == 1;
				
				FireworkEffect.Type effectType = FireworkEffect.Type.valueOf(((NBTTagCompound)tag).getString("fireworkType"));
				
				NBTTagList primary = ((NBTTagCompound)tag).getList("primary");
				NBTTagList secondary = ((NBTTagCompound)tag).getList("fade");
				
				ArrayList<Color> colors = new ArrayList<Color>();
				ArrayList<Color> fade = new ArrayList<Color>();
				
				if(primary != null)
				{
					for(int i = 0; i < primary.size(); ++i)
						colors.add((Color)makeObjectFor(primary.get(i)));
				}
				
				if(secondary != null)
				{
					for(int i = 0; i < secondary.size(); ++i)
						fade.add((Color)makeObjectFor(secondary.get(i)));
				}
				
				return FireworkEffect.builder().flicker(flicker).trail(trail).with(effectType).withColor(colors).withFade(fade).build();
			}
			else
				throw new RuntimeException("Unsupported type: " + type);
		}
		
		else
			throw new RuntimeException("Unsupported tag type: " + tag.getClass().getName());
		
		return data;
	}
	@SuppressWarnings( "unchecked" )
	private NBTBase makeTagFor(Object data)
	{
		NBTBase tag = null;
		if(data instanceof String)
		{
			tag = new NBTTagString("", (String)data);
		}
		else if(data instanceof Integer)
		{
			tag = new NBTTagInt("", (Integer)data);
		}
		else if(data instanceof Double)
		{
			tag = new NBTTagDouble("", (Double)data);
		}
		else if(data instanceof Byte)
		{
			tag = new NBTTagByte("", (Byte)data);
		}
		else if(data instanceof Float)
		{
			tag = new NBTTagFloat("", (Float)data);
		}
		else if(data instanceof Long)
		{
			tag = new NBTTagLong("", (Long)data);
		}
		else if(data instanceof Short)
		{
			tag = new NBTTagShort("", (Short)data);
		}
		else if(data instanceof List)
		{
			tag = new NBTTagList("");
			
			Class<?> lastType = null;
			for(Object obj : ((List<?>)data))
			{
				if(lastType != null && !lastType.isInstance(obj))
					throw new RuntimeException("Can not have multiple types withing a tag list");
				lastType = obj.getClass();
				
				NBTBase subTag = makeTagFor(obj);
				
				((NBTTagList)tag).add(subTag);
			}
		}
		else if(data instanceof Map)
		{
			tag = new NBTTagCompound("");
			((NBTTagCompound)tag).set("type", new NBTTagString("", "map"));
			NBTTagCompound map = new NBTTagCompound("");
			
			for(Entry<String, Object> entry : ((Map<String, Object>)data).entrySet())
			{
				NBTBase subTag = makeTagFor(entry.getValue());
				if(subTag != null)
					map.set(entry.getKey(), subTag);
			}
			
			((NBTTagCompound)tag).set("value", map);
		}
		else if(data instanceof Color)
		{
			tag = new NBTTagCompound("");
			((NBTTagCompound)tag).set("type", new NBTTagString("", "color"));
			((NBTTagCompound)tag).set("value", new NBTTagInt("", ((Color)data).asRGB()));
		}
		else if(data instanceof FireworkEffect)
		{
			FireworkEffect effect = (FireworkEffect)data;
			
			tag = new NBTTagCompound("");
			((NBTTagCompound)tag).set("type", new NBTTagString("", "firework"));
			((NBTTagCompound)tag).set("hasTrail", new NBTTagByte("", (byte)(effect.hasTrail() ? 1 : 0)));
			((NBTTagCompound)tag).set("hasFlicker", new NBTTagByte("", (byte)(effect.hasFlicker() ? 1 : 0)));
			((NBTTagCompound)tag).set("fireworkType", new NBTTagString("", effect.getType().name()));
			
			if(effect.getColors() != null)
			{
				NBTTagList colours = new NBTTagList("");
				for(Color col : effect.getColors())
					colours.add(makeTagFor(col));
				((NBTTagCompound)tag).set("primary", colours);
			}
			
			if(effect.getFadeColors() != null)
			{
				NBTTagList colours = new NBTTagList("");
				for(Color col : effect.getFadeColors())
					colours.add(makeTagFor(col));
				((NBTTagCompound)tag).set("fade", colours);
			}
		}
		else
			throw new RuntimeException("Invalid type " + data.getClass().getName() + " for making NBT tags");
		
		return tag;
	}
	
	public void write(DataOutput output) throws IOException
	{
		if(mMeta == null) // It was an invalid item like air
		{
			output.writeByte(-1);
			return;
		}
		
		output.writeByte(1);
		
		// Make it into a nbt tag
		Map<String,Object> data = mMeta.serialize();
		NBTTagCompound root = (NBTTagCompound)makeTagFor(data);
		root.setName("meta");
		
		NBTBase.writeNamedTag(root, output);
	}
	
	public void read(DataInput input) throws IOException, RecordFormatException
	{
		try
		{
			byte typeid = input.readByte(); 
			
			if(typeid == -1)
				return; // No metadata
			
			NBTTagCompound root = (NBTTagCompound)NBTBase.readNamedTag(input);
			@SuppressWarnings( "unchecked" )
			Map<String,Object> data = (Map<String,Object>)makeObjectFor(root);
			
			Method deserialize = itemMetaDeserializerClass.getDeclaredMethod("deserialize", Map.class);
			
			mMeta = (ItemMeta)deserialize.invoke(null,  data);
			
		}
		catch(IOException e)
		{
			throw new RecordFormatException("Error reading Item Meta. Malformed data.");
		}
		catch ( Exception e )
		{
			throw new RuntimeException(e);
		}
		
	}
	
	public int getSize()
	{
		// It is quite complicated and this is just easier
		try
		{
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			DataOutputStream output = new DataOutputStream(stream);
			
			write(output);
			
			return stream.size();
		}
		catch(IOException e)
		{
			return 0;
		}
	}
	
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof StoredItemMeta))
			return false;
		
		return mMeta.equals(((StoredItemMeta)obj).mMeta);
	}
	
}
