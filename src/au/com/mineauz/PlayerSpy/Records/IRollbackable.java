package au.com.mineauz.PlayerSpy.Records;

public interface IRollbackable 
{
	/**
	 * Gets whether this record has been rolled back
	 */
	public boolean wasRolledBack();
	/**
	 * Sets the state of rollback
	 */
	public void setRolledBack(boolean value);
}
