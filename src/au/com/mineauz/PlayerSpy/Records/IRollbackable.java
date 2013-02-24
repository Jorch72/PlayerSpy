package au.com.mineauz.PlayerSpy.Records;

import org.bukkit.entity.Player;

public interface IRollbackable 
{
	/**
	 * Used so that subtypes of a record dont all have to be able to rollback
	 * @return
	 */
	public boolean canBeRolledBack();
	/**
	 * Gets whether this record has been rolled back
	 */
	public boolean wasRolledBack();
	
	public boolean rollback(boolean preview, Player previewTarget);
	public boolean restore();
	
	/**
	 * Used by the loader to apply its initial state
	 */
	public void setRollbackState(boolean state);
}
