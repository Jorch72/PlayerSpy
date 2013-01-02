package au.com.mineauz.PlayerSpy.storage;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_4_6.inventory.CraftItemFactory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import au.com.mineauz.PlayerSpy.Records.RecordFormatException;

public class StoredItemMeta
{
	private ItemMeta mMeta;
	
	private enum ItemMetaType
	{
		Book(0, BookMeta.class),
		EnchantBook(1, EnchantmentStorageMeta.class),
		FireworkEffect(2, FireworkEffectMeta.class),
		Firework(3, FireworkMeta.class),
		LeatherArmor(4, LeatherArmorMeta.class),
		Map(5, MapMeta.class),
		Potion(6, PotionMeta.class),
		Skull(7, SkullMeta.class),
		Invalid(8, null),
		
		Unspec(-1, null);
		
		public Class<? extends ItemMeta> clazz;
		public int id;
		
		ItemMetaType(int id, Class<? extends ItemMeta> metaClass)
		{
			clazz = metaClass;
			this.id = id;
		}
		
		public static ItemMetaType fromId(int id)
		{
			for(ItemMetaType type : values())
			{
				if(type.id == id)
					return type;
			}
			
			return null;
		}
		
		public static ItemMetaType fromClass(Class<? extends ItemMeta> metaClass)
		{
			for(ItemMetaType type : values())
			{
				if(type.clazz == null)
					continue;
				if(type.clazz.isAssignableFrom(metaClass))
					return type;
			}
			
			return Unspec;
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
	
	public void write(DataOutput output) throws IOException
	{
		if(mMeta == null) // It was an invalid item like air
		{
			output.writeByte(ItemMetaType.Invalid.id);
			return;
		}
		ItemMetaType type = ItemMetaType.fromClass(mMeta.getClass());
		
		output.writeByte(type.id);
		
		// Display name
		output.writeUTF(mMeta.hasDisplayName() ? mMeta.getDisplayName() : "");
		
		// Lore
		if(mMeta.hasLore())
		{
			List<String> lore = mMeta.getLore();
			
			output.writeByte(lore.size());
			for(int i = 0; i < lore.size(); ++i)
				output.writeUTF(lore.get(i));
		}
		else
			output.writeByte(0);
		
		// RepairCost
		// TODO: This is reserved for repair cost but it is not yet exposed in ItemMeta 
		output.writeShort(0);
		
		// Enchants
		Map<Enchantment, Integer> enchants = mMeta.getEnchants();
		output.writeByte(enchants.size());
		
		for(Entry<Enchantment, Integer> entry : enchants.entrySet())
		{
			output.writeShort(entry.getKey().getId());
			output.writeShort(entry.getValue());
		}
		
		// Write specific info
		switch(type)
		{
		case Book:
			output.writeUTF(((BookMeta)mMeta).hasTitle() ? ((BookMeta)mMeta).getTitle() : "");
			output.writeUTF(((BookMeta)mMeta).hasAuthor() ? ((BookMeta)mMeta).getAuthor() : "");
			if(!((BookMeta)mMeta).hasPages())
				output.writeByte(0);
			else
			{
				output.writeByte(((BookMeta)mMeta).getPageCount());
				for(int i = 0; i < ((BookMeta)mMeta).getPageCount(); ++i)
					output.writeUTF(((BookMeta)mMeta).getPage(i));
			}
			break;
		case EnchantBook:
			
			enchants = ((EnchantmentStorageMeta)mMeta).getStoredEnchants();
			output.writeByte(enchants.size());
			
			for(Entry<Enchantment, Integer> entry : enchants.entrySet())
			{
				output.writeShort(entry.getKey().getId());
				output.writeShort(entry.getValue());
			}
			
			break;
		case Firework:
			
			output.writeByte(((FireworkMeta)mMeta).getEffectsSize());
			
			for(FireworkEffect effect : ((FireworkMeta)mMeta).getEffects())
			{
				output.writeByte(effect.getType().ordinal());
				output.writeBoolean(effect.hasFlicker());
				output.writeBoolean(effect.hasTrail());
				
				// Primary colours
				output.writeByte(effect.getColors().size());
				for(Color col : effect.getColors())
					output.writeInt(col.asRGB());
				
				// Fade colours
				output.writeByte(effect.getFadeColors().size());
				for(Color col : effect.getFadeColors())
					output.writeInt(col.asRGB());
			}
			
			break;
		case FireworkEffect:
			output.writeByte(((FireworkEffectMeta)mMeta).getEffect().getType().ordinal());
			output.writeBoolean(((FireworkEffectMeta)mMeta).getEffect().hasFlicker());
			output.writeBoolean(((FireworkEffectMeta)mMeta).getEffect().hasTrail());
			
			// Primary colours
			output.writeByte(((FireworkEffectMeta)mMeta).getEffect().getColors().size());
			for(Color col : ((FireworkEffectMeta)mMeta).getEffect().getColors())
				output.writeInt(col.asRGB());
			
			// Fade colours
			output.writeByte(((FireworkEffectMeta)mMeta).getEffect().getFadeColors().size());
			for(Color col : ((FireworkEffectMeta)mMeta).getEffect().getFadeColors())
				output.writeInt(col.asRGB());
			
			break;
		case LeatherArmor:
			output.writeInt(((LeatherArmorMeta)mMeta).getColor().asRGB());
			break;
		case Map:
			output.writeBoolean(((MapMeta)mMeta).isScaling());
			break;
		case Potion:
			output.writeByte(((PotionMeta)mMeta).getCustomEffects().size());
			
			for(PotionEffect effect : ((PotionMeta)mMeta).getCustomEffects())
			{
				output.writeByte(effect.getType().getId());
				output.writeBoolean(effect.isAmbient());
				output.writeInt(effect.getAmplifier());
				output.writeInt(effect.getDuration());
			}
			break;
		case Skull:
			output.writeUTF(((SkullMeta)mMeta).hasOwner() ? ((SkullMeta)mMeta).getOwner() : "");
			break;
		default:
			break;
		}
		
		
	}
	
	public void read(DataInput input) throws IOException, RecordFormatException
	{
		try
		{
			int typeid = input.readByte(); 
			ItemMetaType type = ItemMetaType.fromId(typeid);
			
			if(type == null)
				throw new RecordFormatException("Bad meta type " + typeid);
			
			if(type == ItemMetaType.Invalid)
				return;
			
			mMeta = getMatchingMeta(type);
			
			String displayName;
			ArrayList<String> lore;
			@SuppressWarnings( "unused" )
			int repairCost;
			
			// The display name
			displayName = input.readUTF();
			if(displayName.length() != 0)
				mMeta.setDisplayName(displayName);
			
			// Lore
			int loreCount = input.readByte();
			if(loreCount < 0)
				throw new RecordFormatException("Bad lore count " + loreCount);
			
			if(loreCount > 0)
			{
				lore = new ArrayList<String>();
				for(int i = 0; i < loreCount; ++i)
					lore.add(input.readUTF());
				
				mMeta.setLore(lore);
			}
			
			// Repair cost
			repairCost = input.readShort();
			// mMeta.setRepairCost(repairCost);
			
			// Enchants
			
			int enchantCount = input.readByte();
			
			if(enchantCount < 0)
				throw new RecordFormatException("Bad enchant count " + enchantCount);
			
			for(int i = 0; i < enchantCount; ++i)
			{
				int enchantId = input.readShort();
				Enchantment ench = Enchantment.getById(enchantId);
				
				if(ench == null)
					throw new RecordFormatException("Bad enchantment id " + enchantId);
				
				mMeta.addEnchant(ench, input.readShort(), true);
			}
			
			String temp = "";
			switch(type)
			{
			case Book:
				temp = input.readUTF();
				if(!temp.isEmpty())
					((BookMeta)mMeta).setTitle(temp);
				
				temp = input.readUTF();
				if(!temp.isEmpty())
					((BookMeta)mMeta).setAuthor(temp);
				
				int pageCount = input.readByte();
				
				if(pageCount < 0)
					throw new RecordFormatException("Bad page count " + pageCount);
				
				if(pageCount > 0)
				{
					ArrayList<String> pages = new ArrayList<String>(pageCount);
					
					for(int i = 0; i < pageCount; ++i)
						pages.add(input.readUTF());
				
					((BookMeta)mMeta).setPages(pages);
				}
				break;
			case EnchantBook:
				
				int storedEnchCount = input.readByte();
				
				if(storedEnchCount < 0)
					throw new RecordFormatException("Bad stored enchant count " + storedEnchCount);
				
				for(int i = 0; i < storedEnchCount; ++i)
				{
					int enchType = input.readShort();
					Enchantment ench = Enchantment.getById(enchType);
					
					if(ench == null)
						throw new RecordFormatException("Bad stored enchantment type " + enchType);
					
					((EnchantmentStorageMeta)mMeta).addStoredEnchant(ench, input.readShort(), true);
				}
				break;
			case Firework:
				
				int effectCount = input.readByte();
				
				if(effectCount < 0)
					throw new RecordFormatException("Bad effect count " + effectCount);
				
				for(int i = 0; i < effectCount; ++i)
				{
					FireworkEffect.Builder builder = FireworkEffect.builder();
					
					int typeId = input.readByte();
					
					if(typeId < 0 || typeId >= FireworkEffect.Type.values().length)
						throw new RecordFormatException("Bad firework effect type " + typeId);
					
					builder.with(FireworkEffect.Type.values()[typeId]);
					
					if(input.readBoolean())
						builder.withFlicker();
					
					if(input.readBoolean())
						builder.withTrail();
					
					// Primary Colours
					int primaryColourCount = input.readByte();
					
					if(primaryColourCount < 0)
						throw new RecordFormatException("Bad colour count " + primaryColourCount);
					
					for(int c = 0; c < primaryColourCount; ++c)
						builder.withColor(Color.fromRGB(input.readInt()));
					
					// Fade Colours
					int fadeColourCount = input.readByte();
					
					if(fadeColourCount < 0)
						throw new RecordFormatException("Bad colour count " + fadeColourCount);
					
					for(int c = 0; c < fadeColourCount; ++c)
						builder.withFade(Color.fromRGB(input.readInt()));
					
					((FireworkMeta)mMeta).addEffect(builder.build());
				}
				
				break;
			case FireworkEffect:
				
				FireworkEffect.Builder builder = FireworkEffect.builder();
				
				int typeId = input.readByte();
				
				if(typeId < 0 || typeId >= FireworkEffect.Type.values().length)
					throw new RecordFormatException("Bad firework effect type " + typeId);
				
				builder.with(FireworkEffect.Type.values()[typeId]);
				
				if(input.readBoolean())
					builder.withFlicker();
				
				if(input.readBoolean())
					builder.withTrail();
				
				// Primary Colours
				int primaryColourCount = input.readByte();
				
				if(primaryColourCount < 0)
					throw new RecordFormatException("Bad colour count " + primaryColourCount);
				
				for(int c = 0; c < primaryColourCount; ++c)
					builder.withColor(Color.fromRGB(input.readInt()));
				
				// Fade Colours
				int fadeColourCount = input.readByte();
				
				if(fadeColourCount < 0)
					throw new RecordFormatException("Bad colour count " + fadeColourCount);
				
				for(int c = 0; c < fadeColourCount; ++c)
					builder.withFade(Color.fromRGB(input.readInt()));
				
				((FireworkEffectMeta)mMeta).setEffect(builder.build());
				
				break;
			case LeatherArmor:
				
				((LeatherArmorMeta)mMeta).setColor(Color.fromRGB(input.readInt()));
				break;
			case Map:
				
				((MapMeta)mMeta).setScaling(input.readBoolean());
				break;
			case Potion:
				
				int potionEffectCount = input.readByte();
				
				if(potionEffectCount < 0)
					throw new RecordFormatException("Bad effect count " + potionEffectCount);
				
				for(int i = 0; i < potionEffectCount; ++i)
				{
					int effectId = input.readByte();
					
					PotionEffectType effectType = PotionEffectType.getById(effectId);
					
					if(effectType == null)
						throw new RecordFormatException("Bad potion effect type " + effectId);
					
					boolean ambient = input.readBoolean();
					int amp = input.readInt();
					int dur = input.readInt();
					
					((PotionMeta)mMeta).addCustomEffect(new PotionEffect(effectType, dur, amp, ambient), true);
				}

				break;
			case Skull:
				temp = input.readUTF();
				if(!temp.isEmpty())
					((SkullMeta)mMeta).setOwner(temp);
				break;
			case Unspec:
				break;
			default:
				break;
			
			}
		}
		catch(UTFDataFormatException e)
		{
			throw new RecordFormatException("Error reading UTF string. Malformed data.");
		}
	}
	
	// A hack to get around the fact that we cant directly construct any of them
	private ItemMeta getMatchingMeta(ItemMetaType type)
	{
		switch(type)
		{
		case Book:
			return CraftItemFactory.instance().getItemMeta(Material.BOOK);
		case EnchantBook:
			return CraftItemFactory.instance().getItemMeta(Material.ENCHANTED_BOOK);
		case Firework:
			return CraftItemFactory.instance().getItemMeta(Material.FIREWORK);
		case FireworkEffect:
			return CraftItemFactory.instance().getItemMeta(Material.FIREWORK_CHARGE);
		case LeatherArmor:
			return CraftItemFactory.instance().getItemMeta(Material.LEATHER_BOOTS);
		case Map:
			return CraftItemFactory.instance().getItemMeta(Material.MAP);
		case Potion:
			return CraftItemFactory.instance().getItemMeta(Material.POTION);
		case Skull:
			return CraftItemFactory.instance().getItemMeta(Material.SKULL_ITEM);
		case Unspec:
		default:
			return CraftItemFactory.instance().getItemMeta(Material.STONE);
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
	
	
	
}
