package au.com.mineauz.PlayerSpy;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;

public class Cause 
{
	private Cause(OfflinePlayer player, String extraCause) 
	{
		mPlayer = player;
		mExtraCause = extraCause;
		mId = -1;
	}
	private Cause(World world, String extraCause)
	{
		mWorld = world;
		mExtraCause = extraCause;
		mId = -1;
	}
	private Cause(int id)
	{
		mId = id;
	}
	
	private OfflinePlayer mPlayer;
	private String mExtraCause;
	private World mWorld;
	
	// Using for placeholders so they can be replaced with the correct info later
	private int mId;
	
	/**
	 * Gets the player who is the cause. Can be null meaning no specific player
	 */
	public OfflinePlayer getCausingPlayer() { return mPlayer; }
	/**
	 * Gets the extra cause info. Can be null if there isnt any. Cannot be null if the player is null
	 */
	public String getExtraCause() { return mExtraCause; }
	
	/**
	 * Sets the extra cause info. Cannot be changed if this isnt a player cause
	 */
	public void setExtraCause(String extraCause) { assert isPlayer(); mExtraCause = extraCause; }
	
	/**
	 * Gets the world the cause is for when this is a global cause.
	 */
	public World getWorld() { return mWorld; }
	
	/**
	 * Gets whether this is a placeholder cause
	 * @return
	 */
	public boolean isPlaceholder() { return mId != -1; }
	
	/**
	 * Gets whether this is a global cause
	 * @return
	 */
	public boolean isGlobal() { return mWorld != null && mExtraCause != null; }
	
	/**
	 * Gets whether this is an unknown cause
	 * @return
	 */
	public boolean isUnknown() { return mPlayer == null && mExtraCause == null && mId == -1; }
	
	/**
	 * Gets whether this is a player cause
	 * @return
	 */
	public boolean isPlayer() { return mPlayer != null; }
	
	/**
	 * Updates this cause with the details from another one
	 */
	public synchronized void update(Cause newCause)
	{
		mPlayer = newCause.mPlayer;
		mExtraCause = newCause.mExtraCause;
		mId = newCause.mId;
	}
	@Override
	public boolean equals(Object obj) 
	{
		if(!(obj instanceof Cause))
			return false;
		
		Cause other = (Cause)obj;
		
		if(isPlaceholder() != other.isPlaceholder())
			return false;
		
		if(isPlaceholder())
		{
			// Match the ids of the placeholders
			if(mId == other.mId)
				return true;
		}
		else
		{
			// Match the owner and extra cause
			if(mPlayer != null && !mPlayer.equals(other.mPlayer))
				return false;
			else if(mPlayer == null && other.mPlayer != null)
				return false;
			else if(mWorld != null && mWorld.equals(other.mWorld))
				return false;
			else if(mWorld == null && other.mWorld != null)
				return false;
			else if(mExtraCause != null && !mExtraCause.equals(other.mExtraCause))
				return false;
			else if(mExtraCause == null && other.mExtraCause != null)
				return false;
			
			return true;
		}

		return true;
	}
	
	@Override
	public int hashCode() 
	{
		if(isPlaceholder())
			return mId | (1 << 31);

		int hash = 0;
		if(mPlayer != null)
			hash = mPlayer.getName().hashCode();
		else if(mWorld != null)
			hash = mWorld.hashCode();
		if(mExtraCause != null)
			hash ^= mExtraCause.hashCode();
		
		return hash;
	}
	
	@Override
	public String toString() 
	{
		if(isPlaceholder())
			return "Placeholder Cause: " + mId;
		
		if(isGlobal())
			return "Global Cause: " + mWorld.getName() + "-" + mExtraCause;
		else if(isUnknown())
			return "Unknown Cause";
		else
		{
			if(mExtraCause != null)
				return "Player Cause: " + Utility.formatName(mPlayer.getName(), mExtraCause);
			else
				return "Player Cause: " + mPlayer.getName();
		}

	}
	
	/**
	 * Creates a cause that is by a player
	 * @param causingPlayer The player causing it. Cannot be null
	 * @param extraCause Any extra cause. Cannot be null
	 */
	public static Cause playerCause(OfflinePlayer causingPlayer, String extraCause)
	{
		assert causingPlayer != null : "causingPlayer cannot be null";
		assert extraCause != null;
		return new Cause(causingPlayer, extraCause);
	}
	/**
	 * Creates a cause that is by a player
	 * @param causingPlayer The player causing it. Cannot be null
	 */
	public static Cause playerCause(OfflinePlayer causingPlayer)
	{
		assert causingPlayer != null : "causingPlayer cannot be null";
		return new Cause(causingPlayer, null);
	}
	
	/**
	 * Creates a cause that is not caused by a player
	 * @param world The world its in. Cannot be null
	 * @param cause The cause. Cannot be null
	 */
	public static Cause globalCause(World world, String cause)
	{
		assert world != null : "world cannot be null for a global cause";
		assert cause != null : "cause cannot be null for a global cause";
		
		return new Cause(world, cause);
	}
	
	/**
	 * Gets a placeholder cause that can be used to log records without currently knowing the cause 
	 * @return
	 */
	public static Cause placeholderCause()
	{
		return new Cause(sPlaceHolderId++);
	}
	private static int sPlaceHolderId = 0;
	
	/**
	 * Gets an unknown cause
	 * @return
	 */
	public static Cause unknownCause()
	{
		return new Cause(-1);
	}
	
}
