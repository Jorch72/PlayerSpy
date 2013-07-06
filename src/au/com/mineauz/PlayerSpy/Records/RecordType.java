package au.com.mineauz.PlayerSpy.Records;

public enum RecordType
{
	// has 1 argument: the location where they spawned
	Login,
	// 2 argument: the type of logoff, 0 = quit, 1 = kick, 2 = ban, the 2nd argument is the reason or null if quit
	Logoff,
	// 1 argument: the name of the world
	WorldChange,
	// 2 argument: the location they moved to, angle of the head 
	Move,
	// 2 argument: the item stack they picked up, the location of the drop before pickup
	ItemPickup,
	// 2 arguments: the type of interact, the entity/block they interacted with or null
	Interact,
	// 1 argument: the message
	ChatCommand,
	// 1 argument: the location
	Teleport,
	// 1 argument: the game mode
	GameMode,
	// 1 argument: the location they died
	Death,
	// 1 argument: the location they respawned
	Respawn,
	// no arguments
	EndOfSession,
	// no arguments
	ArmSwing,
	// 1 argument: boolean on/off
	Sneak,
	// 1 argument: boolean on/off
	Sprint,
	// 2 arguments: 2 StoredBlock objects, from block and to block
	BlockChange,
	// 2 arguments: the inventory size, array of inventory ItemStacks
	FullInventory,
	// 2 arguments: the slot id, the itemstack
	UpdateInventory,
	Damage,
	Attack, 
	HeldItemChange,
	DropItem,
	PaintingChange,
	VehicleMount,
	Sleep,
	// This is like eating or throwing enderpearls etc.
	RClickAction,
	ItemTransaction,
	ItemFrameChange,
	EntitySpawn
}
