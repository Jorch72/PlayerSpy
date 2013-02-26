package au.com.mineauz.PlayerSpy.rollback;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.LogTasks.MarkRecordRollbackStateTask;
import au.com.mineauz.PlayerSpy.Records.IRollbackable;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.search.NoRolledbackConstraint;
import au.com.mineauz.PlayerSpy.search.SearchFilter;
import au.com.mineauz.PlayerSpy.search.SearchTask;

public class RollbackManager
{
	public static final RollbackManager instance;
	
	private ArrayList<RollbackSession> mSessions = new ArrayList<RollbackSession>();
	
	static
	{
		instance = new RollbackManager();
	}
	private RollbackManager()
	{
		
	}
	
	/**
	 * Starts a rollback using a specified filter
	 * @param filter What to rollback
	 * @param notifyPlayer The player who will be notified, can be null unless preview is set
	 * @param preview Whether to actually do the rollback, or do it client side only. If this is set, then notifyPlayer cannot be null
	 */
	public void startRollback(SearchFilter filter, Player notifyPlayer, boolean preview)
	{
		assert filter != null;
		assert (preview && notifyPlayer != null) || !preview;
		
		RollbackSession session = new RollbackSession();
		
		filter.andConstraints.add(new NoRolledbackConstraint());
		session.filter = filter;
		session.notifyPlayer = notifyPlayer;
		session.preview = preview;
		
		session.restore = false;
		
		session.future = SpyPlugin.getExecutor().submit(new SearchTask(filter));
		
		session.changed = 0;
		session.failed = 0;
		
		session.modified = new ArrayList<Record>();
		
		mSessions.add(session);
		
		if(notifyPlayer != null)
			notifyPlayer.sendMessage(ChatColor.GOLD + "[PlayerSpy] " + ChatColor.WHITE + " Starting rollback of " + filter.getDescription());
	}
	
	/**
	 * Starts a rollback using a specified filter
	 * @param filter What to rollback
	 */
	public void startRollback(SearchFilter filter)
	{
		startRollback(filter, null, false);
	}
	
	/**
	 * Undoes a rollback. 
	 * @param session the rollback to undo. it must not be a rollback preview
	 * @param notifyPlayer
	 */
	public void undoRollback(RollbackSession session, Player notifyPlayer)
	{
		assert session != null;
		assert !session.preview;
		assert session.results != null;
		
		session.notifyPlayer = notifyPlayer;
		session.preview = false;
		
		session.restore = true;
		session.progress = session.results.allRecords.size()-1;
		
		session.changed = 0;
		session.failed = 0;
		
		session.modified = new ArrayList<Record>();
		
		mSessions.add(session);
	}
	
	public void undoRollback(RollbackSession session)
	{
		undoRollback(session, null);
	}
	
	public void update()
	{
		int updates = 0;
		
		for(int i = 0; i < mSessions.size(); i++)
		{
			RollbackSession session = mSessions.get(i);
			
			if(session.results == null && session.future.isDone())
			{
				try
				{
					session.results = session.future.get();
					if(session.restore)
						session.progress = session.results.allRecords.size()-1;
					else
						session.progress = 0;
					
					// TODO: compact results to end results only
				}
				catch(CancellationException e)
				{
					mSessions.remove(i);
					i--;
					continue;
				}
				catch ( InterruptedException e )
				{
					mSessions.remove(i);
					i--;
					continue;
				}
				catch ( ExecutionException e )
				{
					e.printStackTrace();
					
					mSessions.remove(i);
					i--;
					continue;
				}
			}
			
			if(session.results != null)
			{
				while((session.restore && session.progress >= 0) || (!session.restore && session.progress < session.results.allRecords.size()))
				{
					if(session.results.allRecords.get(session.progress).getArg1() instanceof IRollbackable)
					{
						IRollbackable record = (IRollbackable)session.results.allRecords.get(session.progress).getArg1();
						
						if(session.restore)
						{
							if(record.canBeRolledBack() && record.wasRolledBack())
							{
								if(record.restore())
								{
									updates++;
									session.changed++;
									
									if(((Record)record).sourceEntry != null)
									{
										session.modified.add((Record)record);
									}
								}
								else
									session.failed++;
							}
						}
						else
						{
							if(record.canBeRolledBack() && !record.wasRolledBack())
							{
								if(record.rollback(session.preview, session.notifyPlayer))
								{
									updates++;
									session.changed++;
									
									if(((Record)record).sourceEntry != null)
									{
										session.modified.add((Record)record);
									}
								}
								else
									session.failed++;
							}
						}
					}
					
					if(session.restore)
						session.progress--;
					else
						session.progress++;
					
					if(updates >= SpyPlugin.getSettings().maxChangesPerTick)
						return;
				}
				
				MarkRecordRollbackStateTask task = new MarkRecordRollbackStateTask(session.modified, !session.restore);
				SpyPlugin.getExecutor().submit(task);

				if(session.notifyPlayer != null)
				{
					if(session.restore)
						session.notifyPlayer.sendMessage(ChatColor.GOLD + "[PlayerSpy] " + ChatColor.WHITE + "Restore complete" + (session.failed > 0 ? ". " + session.failed + " records could not be restored." : ""));
					else
						session.notifyPlayer.sendMessage(ChatColor.GOLD + "[PlayerSpy] " + ChatColor.WHITE + "Rollback complete" + (session.failed > 0 ? ". " + session.failed + " records could not be rolled back." : ""));
				}
				else
				{
					if(session.restore)
						LogUtil.info("Restore complete" + (session.failed > 0 ? ". " + session.failed + " records could not be restored." : ""));
					else
						LogUtil.info("Rollback complete" + (session.failed > 0 ? ". " + session.failed + " records could not be rolled back." : ""));
				}
				
				mSessions.remove(i);
				i--;
				continue;
			}
		}
	}
}
