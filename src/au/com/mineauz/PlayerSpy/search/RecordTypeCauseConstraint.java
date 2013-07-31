package au.com.mineauz.PlayerSpy.search;

import java.util.List;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.search.interfaces.CauseConstraint;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class RecordTypeCauseConstraint extends CauseConstraint
{
	private boolean mAllowPlayer;
	private boolean mAllowExtra;
	
	public RecordTypeCauseConstraint(List<Constraint> constraints)
	{
		mAllowPlayer = false;
		mAllowExtra = false;
		
		for(Constraint constraint : constraints)
		{
			if(constraint instanceof RecordTypeConstraint)
			{
				RecordTypeConstraint c = (RecordTypeConstraint)constraint;
				
				switch(c.type)
				{
				case ArmSwing:
				case Attack:
				case ChatCommand:
				case Damage:
				case Death:
				case DropItem:
				case EndOfSession:
				case EntitySpawn:
				case FullInventory:
				case GameMode:
				case HeldItemChange:
				case Interact:
				case ItemPickup:
				case ItemTransaction:
				case Login:
				case Logoff:
				case Move:
				case RClickAction:
				case Respawn:
				case Sleep:
				case Sneak:
				case Sprint:
				case Teleport:
				case UpdateInventory:
				case VehicleMount:
				case WorldChange:
					mAllowPlayer = true;
					break;
					
				case BlockChange:
				case PaintingChange:
				case ItemFrameChange:
				default:
					mAllowPlayer = true;
					mAllowExtra = true;
					break;
				}
			}
		}
	}

	@Override
	public boolean matches( Cause cause )
	{
		if(!mAllowPlayer && cause.isPlayer())
			return false;
		
		if(!mAllowExtra && (cause.isGlobal() || (cause.isPlayer() && cause.getExtraCause() != null)))
			return false;
		
		return true;
	}

	@Override
	public String getDescription()
	{
		return null;
	}

}
