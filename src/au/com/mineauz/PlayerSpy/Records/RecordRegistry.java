package au.com.mineauz.PlayerSpy.Records;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;


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
	public static Record makeRecord(int version, int type)
	{
		if(mMap == null)
			return null;
		
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
				return null;
		}

		// Try to match the record type and version
		while(true)
		{
			// This version has one
			if(recordMap.containsKey(type))
			{
				try
				{
					// Has it been removed in this version?
					if(recordMap.get(type) == null)
						return null;
					
					//LogUtil.finest("Load Record. Type: " + recordMap.get(type).getSimpleName());
					return recordMap.get(type).newInstance();
				}
				catch(IllegalAccessException e)
				{
					e.printStackTrace();
				}
				catch(InstantiationException e)
				{
					e.printStackTrace();
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
				return null;
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
	}
}
