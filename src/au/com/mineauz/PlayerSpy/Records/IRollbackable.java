package au.com.mineauz.PlayerSpy.Records;

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
	/**
	 * Sets the state of rollback
	 */
	public void setRolledBack(boolean value);
}
