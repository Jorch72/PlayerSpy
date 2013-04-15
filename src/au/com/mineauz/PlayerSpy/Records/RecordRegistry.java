package au.com.mineauz.PlayerSpy.Records;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import au.com.mineauz.PlayerSpy.legacy.v2.BlockChangeRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.DropItemRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.InteractRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.InventoryRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.InventoryTransactionRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.ItemFrameChangeRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.ItemPickupRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.PaintingChangeRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.RightClickActionRecord;
import au.com.mineauz.PlayerSpy.legacy.v2.UpdateInventoryRecord;


@SuppressWarnings("deprecation")
public class RecordRegistry 
{
	/**
	 * Registeres a record type
	 * @param version The version this type is introduced in
	 * @param type The type of the record
	 * @param recordClass The class of the record
	 */
	public static void registerRecordType(int version, RecordType type, Class<? extends Record> recordClass)
	{
		registerRecordType(version, type.ordinal(), recordClass);
	}
	/**
	 * Registers a record type.
	 * @param version The version this type is introduced in
	 * @param type The type id of the record
	 * @param recordClass The class of the record
	 */
	public static void registerRecordType(int version, int type, Class<? extends Record> recordClass)
	{
		if(mMap == null)
			mMap = new TreeMap<Integer, HashMap<Integer,Class<? extends Record>>>();
		
		if(!mMap.containsKey(version))
			mMap.put(version, new HashMap<Integer, Class<? extends Record>>());
		
		mMap.get(version).put(type, recordClass);
	}
	
	/**
	 * Tries to create an instance of a record
	 * @param version The highest version to allow
	 * @param type The type id of the record
	 * @return Either an instance of Record, or null if that type has no match
	 */
	public static Record makeRecord(int version, int type) throws RecordFormatException
	{
		if(mMap == null)
			throw new IllegalStateException("Not Initialized");
		
		HashMap<Integer, Class<? extends Record>> recordMap = null;
		if(mMap.containsKey(version))
			recordMap = mMap.get(version);
		else
		{
			Entry<Integer, HashMap<Integer, Class<? extends Record>>> ent = mMap.lowerEntry(version);
			if(ent != null)
			{
				recordMap = ent.getValue();
				version = ent.getKey();
			}
			else
				throw new IllegalArgumentException("Illegal version " + version);
		}

		// Try to match the record type and version
		while(true)
		{
			// This version has one
			if(recordMap.containsKey(type))
			{
				try
				{
					return recordMap.get(type).newInstance();
				}
				catch(IllegalAccessException e)
				{
					throw (RecordFormatException)new RecordFormatException("Unable to access the default constructor for " + recordMap.get(type).getName()).initCause(e);
				}
				catch(InstantiationException e)
				{
					throw (RecordFormatException)new RecordFormatException("Error while instanciating type " + recordMap.get(type).getName()).initCause(e);
				}
			}
			
			// Move down a version
			Entry<Integer, HashMap<Integer, Class<? extends Record>>> ent = mMap.lowerEntry(version);
			if(ent != null)
			{
				recordMap = ent.getValue();
				version = ent.getKey();
			}
			else
				// No record type was found
				throw new RecordFormatException("Unknown record type " + type);
		}
	}
	
	private static TreeMap<Integer, HashMap<Integer, Class<? extends Record>>> mMap;
	
	static
	{
		// Register record types
		
		// ===== Version 1 Records =====
		registerRecordType(1, RecordType.ArmSwing, ArmSwingRecord.class);
		registerRecordType(1, RecordType.Attack, AttackRecord.class);
		registerRecordType(1, RecordType.BlockChange, au.com.mineauz.PlayerSpy.legacy.BlockChangeRecord.class);
		registerRecordType(1, RecordType.ChatCommand, ChatCommandRecord.class);
		registerRecordType(1, RecordType.Damage, DamageRecord.class);
		registerRecordType(1, RecordType.Death, DeathRecord.class);
		registerRecordType(1, RecordType.DropItem, DropItemRecord.class);
		registerRecordType(1, RecordType.EndOfSession, SessionInfoRecord.class);
		registerRecordType(1, RecordType.FullInventory, InventoryRecord.class);
		registerRecordType(1, RecordType.GameMode, GameModeRecord.class);
		registerRecordType(1, RecordType.HeldItemChange, HeldItemChangeRecord.class);
		registerRecordType(1, RecordType.Interact, au.com.mineauz.PlayerSpy.legacy.InteractRecord.class);
		registerRecordType(1, RecordType.ItemPickup, ItemPickupRecord.class);
		registerRecordType(1, RecordType.Login, LoginRecord.class);
		registerRecordType(1, RecordType.Logoff, LogoffRecord.class);
		registerRecordType(1, RecordType.Move, MoveRecord.class);
		registerRecordType(1, RecordType.PaintingChange, PaintingChangeRecord.class);
		registerRecordType(1, RecordType.RClickAction, RightClickActionRecord.class);
		registerRecordType(1, RecordType.Respawn, RespawnRecord.class);
		registerRecordType(1, RecordType.Sleep, SleepRecord.class);
		registerRecordType(1, RecordType.Sneak, SneakRecord.class);
		registerRecordType(1, RecordType.Sprint, SprintRecord.class);
		registerRecordType(1, RecordType.Teleport, TeleportRecord.class);
		registerRecordType(1, RecordType.UpdateInventory, au.com.mineauz.PlayerSpy.legacy.UpdateInventoryRecord.class);
		registerRecordType(1, RecordType.VehicleMount, VehicleMountRecord.class);
		registerRecordType(1, RecordType.WorldChange, WorldChangeRecord.class);
		
		// ===== Version 2 Records =====
		registerRecordType(2, RecordType.UpdateInventory, UpdateInventoryRecord.class);
		registerRecordType(2, RecordType.BlockChange, BlockChangeRecord.class);
		registerRecordType(2, RecordType.Interact, InteractRecord.class);
		registerRecordType(2, RecordType.ItemTransaction, InventoryTransactionRecord.class);
		registerRecordType(2, RecordType.ItemFrameChange, ItemFrameChangeRecord.class);
		
		// ===== Version 3 Records =====
		registerRecordType(3, RecordType.DropItem, au.com.mineauz.PlayerSpy.Records.DropItemRecord.class);
		registerRecordType(3, RecordType.Interact, au.com.mineauz.PlayerSpy.Records.InteractRecord.class);
		registerRecordType(3, RecordType.FullInventory, au.com.mineauz.PlayerSpy.Records.InventoryRecord.class);
		registerRecordType(3, RecordType.ItemTransaction, au.com.mineauz.PlayerSpy.Records.InventoryTransactionRecord.class);
		registerRecordType(3, RecordType.ItemPickup, au.com.mineauz.PlayerSpy.Records.ItemPickupRecord.class);
		registerRecordType(3, RecordType.RClickAction, au.com.mineauz.PlayerSpy.Records.RightClickActionRecord.class);
		registerRecordType(3, RecordType.ItemFrameChange, au.com.mineauz.PlayerSpy.Records.ItemFrameChangeRecord.class);
		registerRecordType(3, RecordType.UpdateInventory, au.com.mineauz.PlayerSpy.Records.UpdateInventoryRecord.class);
		registerRecordType(3, RecordType.BlockChange, au.com.mineauz.PlayerSpy.Records.BlockChangeRecord.class);
		registerRecordType(3, RecordType.PaintingChange, au.com.mineauz.PlayerSpy.Records.PaintingChangeRecord.class);
	}
}
