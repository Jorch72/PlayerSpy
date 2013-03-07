package au.com.mineauz.PlayerSpy.wrappers;

import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.wrappers.craftbukkit.*;
import au.com.mineauz.PlayerSpy.wrappers.minecraft.*;
import au.com.mineauz.PlayerSpy.wrappers.nbt.*;
import au.com.mineauz.PlayerSpy.wrappers.packet.*;

public class PreLoadValidator
{
	public static void validateWrappers()
	{
		try
		{
			AutoWrapper.initialize(CraftInventoryPlayer.class);
			AutoWrapper.initialize(CraftItemStack.class);
			AutoWrapper.initialize(CraftPlayer.class);
			AutoWrapper.initialize(CraftSound.class);
			AutoWrapper.initialize(CraftWorld.class);
			
			AutoWrapper.initialize(DataWatcher.class);
			AutoWrapper.initialize(Entity.class);
			AutoWrapper.initialize(EntityHuman.class);
			AutoWrapper.initialize(EntityItem.class);
			AutoWrapper.initialize(EntityLiving.class);
			AutoWrapper.initialize(EntityPlayer.class);
			AutoWrapper.initialize(EntityShadowPlayer.class);
			AutoWrapper.initialize(EntityTypes.class);
			AutoWrapper.initialize(Item.class);
			AutoWrapper.initialize(ItemPotion.class);
			AutoWrapper.initialize(ItemStack.class);
			AutoWrapper.initialize(MobEffect.class);
			AutoWrapper.initialize(PlayerConnection.class);
			AutoWrapper.initialize(PlayerInventory.class);
			AutoWrapper.initialize(PotionBrewer.class);
			AutoWrapper.initialize(World.class);
			AutoWrapper.initialize(WorldServer.class);
			
			AutoWrapper.initialize(NBTCompressedStreamTools.class);
			AutoWrapper.initialize(NBTBase.class);
			AutoWrapper.initialize(NBTTagByte.class);
			AutoWrapper.initialize(NBTTagCompound.class);
			AutoWrapper.initialize(NBTTagDouble.class);
			AutoWrapper.initialize(NBTTagFloat.class);
			AutoWrapper.initialize(NBTTagInt.class);
			AutoWrapper.initialize(NBTTagList.class);
			AutoWrapper.initialize(NBTTagLong.class);
			AutoWrapper.initialize(NBTTagShort.class);
			AutoWrapper.initialize(NBTTagString.class);
			
			AutoWrapper.initialize(Packet.class);
			AutoWrapper.initialize(Packet18ArmAnimation.class);
			AutoWrapper.initialize(Packet20NamedEntitySpawn.class);
			AutoWrapper.initialize(Packet22Collect.class);
			AutoWrapper.initialize(Packet23VehicleSpawn.class);
			AutoWrapper.initialize(Packet24MobSpawn.class);
			AutoWrapper.initialize(Packet28EntityVelocity.class);
			AutoWrapper.initialize(Packet29DestroyEntity.class);
			AutoWrapper.initialize(Packet32EntityLook.class);
			AutoWrapper.initialize(Packet34EntityTeleport.class);
			AutoWrapper.initialize(Packet35EntityHeadRotation.class);
			AutoWrapper.initialize(Packet40EntityMetadata.class);
			AutoWrapper.initialize(Packet53BlockChange.class);
			AutoWrapper.initialize(Packet5EntityEquipment.class);
			AutoWrapper.initialize(Packet62NamedSoundEffect.class);
		}
		catch(RuntimeException e)
		{
			LogUtil.severe("!!!!!!!  Version mismatch  !!!!!!!");
			LogUtil.severe("Required MC version: 1.4.6 or 1.4.7");
			LogUtil.severe("Recommended CraftBukkit version: 1.4.7 Beta 1");
			
			Throwable ex = e;
			while (ex.getCause() != null)
				ex = ex.getCause();
			
			throw new WrapperValidationException(ex.getMessage());
		}
	}
}
