package au.com.mineauz.PlayerSpy.debugging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.bukkit.entity.Player;

import com.google.common.collect.Multimap;

import au.com.mineauz.PlayerSpy.LogUtil;
import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.SpyPlugin;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.ACIDRandomAccessFile;
import au.com.mineauz.PlayerSpy.Utilities.CubicChunk;
import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.Utilities.Util;
import au.com.mineauz.PlayerSpy.globalreference.FileEntry;
import au.com.mineauz.PlayerSpy.globalreference.GRFileHeader;
import au.com.mineauz.PlayerSpy.globalreference.GlobalReferenceFile;
import au.com.mineauz.PlayerSpy.structurefile.HoleEntry;
import au.com.mineauz.PlayerSpy.structurefile.Index;
import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;
import au.com.mineauz.PlayerSpy.structurefile.StructuredFile;
import au.com.mineauz.PlayerSpy.tracdata.ChunkEntry;
import au.com.mineauz.PlayerSpy.tracdata.ChunkIndex;
import au.com.mineauz.PlayerSpy.tracdata.FileHeader;
import au.com.mineauz.PlayerSpy.tracdata.HoleIndex;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;
import au.com.mineauz.PlayerSpy.tracdata.OwnerMapEntry;
import au.com.mineauz.PlayerSpy.tracdata.RollbackEntry;
import au.com.mineauz.PlayerSpy.tracdata.RollbackIndex;
import au.com.mineauz.PlayerSpy.tracdata.SessionEntry;

public class Debug 
{
	private static Logger mDebugLog;
	private static File mDebugLogFile;
	private static Handler mHandler;
	
	private static HashMap<Player, Level> mDebugReceiver = new HashMap<Player, Level>();
	
	private static OutputStreamWriter mLayoutWriter;
	private static HashMap<Long, String> mLastMessage = new HashMap<Long, String>();
	
	private static HashMap<Long, HashMap<String, Object>> mVariables = new HashMap<Long, HashMap<String,Object>>();
	
	public static synchronized void init(File debugLogFile)
	{
		mDebugLogFile = debugLogFile;
		try
		{
			OutputStream stream = new FileOutputStream(debugLogFile, true);

			mDebugLog = Logger.getLogger("PlayerSpyDebug");
			mDebugLog.setLevel(Level.INFO); // TODO: NOTE: This level should be set to INFO for release versions 
			mDebugLog.setUseParentHandlers(false);
			
			for(Handler handler : mDebugLog.getHandlers())
				mDebugLog.removeHandler(handler);
			
			mHandler = new StreamHandler(stream, new DebugLogFormatter());
			mHandler.setLevel(Level.FINEST);
			mDebugLog.addHandler(mHandler);
			
			Handler chatHandler = new ChatOutputHandler(mDebugReceiver);
			chatHandler.setLevel(Level.ALL);
			mDebugLog.addHandler(chatHandler);
		}
		catch(IOException e)
		{
			LogUtil.severe("RUNNING WITHOUT DEBUG LOG. Reason:");
			e.printStackTrace();
		}
	}
	
	public static synchronized void clearLog()
	{
		mDebugLog.removeHandler(mHandler);
		mHandler.close();
		
		try
		{
			mDebugLogFile.delete();
			
			OutputStream stream = new FileOutputStream(mDebugLogFile);

			mHandler = new StreamHandler(stream, new DebugLogFormatter());
			mHandler.setLevel(Level.FINEST);
			mDebugLog.addHandler(mHandler);
		}
		catch(IOException e)
		{
			LogUtil.severe("RUNNING WITHOUT DEBUG LOG. Reason:");
			e.printStackTrace();
		}
	}
	
	public static synchronized void logException(Throwable e)
	{
		if(mDebugLog != null)
			mDebugLog.log(Level.SEVERE, "Caught exception", e);
		SpyPlugin.getInstance().getLogger().log(Level.SEVERE, "Caught exception", e);
	}
	
	public static synchronized void logExceptionDebugOnly(Throwable e)
	{
		if(mDebugLog != null)
			mDebugLog.log(Level.SEVERE, "Caught exception", e);
	}
	
	public static synchronized void logCrashDebugOnly(CrashReporter crash)
	{
		if(mDebugLog != null)
			crash.log(mDebugLog);
	}
	
	public static synchronized void logCrash(CrashReporter crash)
	{
		if(mDebugLog != null)
			crash.log(mDebugLog);
		crash.log(SpyPlugin.getInstance().getLogger());
	}
	
	public static Logger getDebugLog()
	{
		return mDebugLog;
	}
	
	public static synchronized void loggedAssert(boolean condition, String errorMsg)
	{
		if(condition)
			return;

		if(mDebugLog != null)
		{
			StackTraceElement[] stack = new Throwable().getStackTrace();
			
			String sourceClass = "";
			String sourceMethod = "";
			String sourceString = "";
			
			if(stack.length >= 2)
			{
				sourceClass = stack[1].getClassName();
				sourceMethod = stack[1].getMethodName();
				
				sourceString = String.format("%s:%d", (stack[1].getFileName() == null ? "Unknown Source" : stack[1].getFileName()), (stack[1].getLineNumber() < 0 ? -1 : stack[1].getLineNumber()));
			}
			
			mDebugLog.logp(Level.SEVERE, sourceClass, sourceMethod, "ASSERTION FAILED(" + sourceString + "): " + errorMsg, new AssertionError(errorMsg));
		}
		
		throw new AssertionError(errorMsg);
	}
	public static synchronized void loggedAssert(boolean condition)
	{
		if(condition)
			return;

		if(mDebugLog != null)
		{
			StackTraceElement[] stack = new Throwable().getStackTrace();
			
			String sourceClass = "";
			String sourceMethod = "";
			String sourceString = "";
			
			if(stack.length >= 2)
			{
				sourceClass = stack[1].getClassName();
				sourceMethod = stack[1].getMethodName();
				
				sourceString = String.format("%s:%d", (stack[1].getFileName() == null ? "Unknown Source" : stack[1].getFileName()), (stack[1].getLineNumber() < 0 ? -1 : stack[1].getLineNumber()));
			}
			
			mDebugLog.logp(Level.SEVERE, sourceClass, sourceMethod, "ASSERTION FAILED(" + sourceString + "): ", new AssertionError());
		}
		
		throw new AssertionError();
	}
	
	public static synchronized void info(String text, Object... args)
	{
		if(mDebugLog != null)
			log(Level.INFO, String.format(text, args));
	}
	
	public static synchronized void warning(String text, Object... args)
	{
		if(mDebugLog != null)
			log(Level.WARNING, String.format(text, args));
	}
	
	public static synchronized void severe(String text, Object... args)
	{
		if(mDebugLog != null)
			log(Level.SEVERE, String.format(text, args));
	}
	
	public static synchronized void fine(String text, Object... args)
	{
		if(mDebugLog != null)
			log(Level.FINE, String.format(text, args));
	}
	
	public static synchronized void finer(String text, Object... args)
	{
		if(mDebugLog != null)
			log(Level.FINER, String.format(text, args));
	}
	
	public static synchronized void finest(String text, Object... args)
	{
		if(mDebugLog != null)
			log(Level.FINEST, String.format(text, args));
	}
	
	public static synchronized void aquire(String lockName)
	{
		if(mDebugLog != null)
		{
			finest(Thread.currentThread().getName() + " aquired " + lockName + " lock");
		}
	}
	public static synchronized void unaquire(String lockName)
	{
		if(mDebugLog != null)
		{
			finest(Thread.currentThread().getName() + " unaquired " + lockName + " lock");
		}
	}
	
	public static synchronized void setDebugLevel(Player player, Level level)
	{
		if(level == Level.OFF)
			mDebugReceiver.remove(player);
		else
			mDebugReceiver.put(player, level);
	}
	
	private static void log(Level level, String message)
	{
		// Find the source
		StackTraceElement[] stack = new Throwable().getStackTrace();
		
		StackTraceElement[] trace = stack;
		
		String sourceClass = null;
		String sourceMethod = null;
		
		for(int i = 0; i < stack.length; ++i)
		{
			sourceClass = stack[i].getClassName();
			
			if(sourceClass.equals(Debug.class.getName()))
				continue;
			
			sourceMethod = stack[i].getMethodName();
			trace = Arrays.copyOfRange(stack, i, stack.length);
			break;
		}
		
		mDebugLog.logp(level, sourceClass, sourceMethod, message, new Object[] {trace});
		mLastMessage.put(Thread.currentThread().getId(), message);
	}
	
	public static void logLayout(StructuredFile log)
	{
		// This is not to be used for release versions
		if(log instanceof LogFile)
			logLayoutInt((LogFile)log);
		else
			logLayoutInt((GlobalReferenceFile)log);
	}
	private static void logLayoutInt(LogFile log)
	{
		if(!log.getName().equals("testAppend"))
			return;
		
		try
		{
			File f = new File("plugins/PlayerSpy/layout.txt");
			if(!f.exists())
				f.getParentFile().mkdirs();
			
			if(mLayoutWriter == null)
				mLayoutWriter = new OutputStreamWriter(new FileOutputStream(f));
			
			HoleIndex holeIndex;
			RollbackIndex rollbackIndex;
			FileHeader header;
			ACIDRandomAccessFile file;
			
			try
			{
				Field field = log.getClass().getDeclaredField("mHoleIndex");
				field.setAccessible(true);
				
				holeIndex = (HoleIndex) field.get(log);
				
				field = log.getClass().getDeclaredField("mHeader");
				field.setAccessible(true);
				
				header = (FileHeader)field.get(log);
				
				field = log.getClass().getDeclaredField("mRollbackIndex");
				field.setAccessible(true);
				
				rollbackIndex = (RollbackIndex) field.get(log);
				
				field = StructuredFile.class.getDeclaredField("mFile");
				field.setAccessible(true);
				
				file = (ACIDRandomAccessFile) field.get(log);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return;
			}
			
			mLayoutWriter.write("\r\nLayout: ");
			String message = mLastMessage.get(Thread.currentThread().getId());
			if(message != null)
				mLayoutWriter.write(message);

			mLayoutWriter.write("\r\n");
			
			TreeMap<Long,Pair<Long,Object>> sortedItems = new TreeMap<Long, Pair<Long,Object>>();
			sortedItems.put(0L, new Pair<Long,Object>((long)header.getSize(),"Header"));
			
			sortedItems.put(header.IndexLocation, new Pair<Long,Object>(header.IndexSize,"Session Index"));
			sortedItems.put(header.HolesIndexLocation, new Pair<Long,Object>(header.HolesIndexSize,"Holes Index (" + header.HolesIndexPadding + ")"));
			sortedItems.put(header.OwnerMapLocation, new Pair<Long,Object>(header.OwnerMapSize,"OwnerMap"));
			sortedItems.put(header.RollbackIndexLocation, new Pair<Long,Object>(header.RollbackIndexSize,"RollbackIndex"));
			
			for(HoleEntry hole : holeIndex)
				sortedItems.put(hole.Location, new Pair<Long,Object>(hole.Size,"Hole"));
			
			for(RollbackEntry entry : rollbackIndex)
				sortedItems.put(entry.detailLocation, new Pair<Long,Object>(entry.detailSize,"RollbackDetail for Session " + entry.sessionId));
			
			for(SessionEntry session : log.getSessions())
			{
				String tag = log.getOwnerTag(session);
				
				sortedItems.put(session.Location, new Pair<Long,Object>(session.TotalSize,"Session " + (tag != null ? tag + "(" + session.Id + ")" : session.Id) + " Padding: " + session.Padding));
			}
			
			// Find any Unallocated space
			long lastPos = 0;
			String last = "";
			for(Entry<Long, Pair<Long, Object>> entry : sortedItems.entrySet())
			{
				if(lastPos > entry.getKey())
					mLayoutWriter.write(String.format("%X-%X:\t\tCONFLICT with %s!\r\n", entry.getKey(), entry.getKey(), last));
				else if(lastPos < entry.getKey())
				{
					mLayoutWriter.write(String.format("%X-%X:\t\tUnallocated space!\r\n", lastPos, entry.getKey() - 1));
					lastPos = entry.getKey() + entry.getValue().getArg1();
					last = (String)entry.getValue().getArg2();
				}
				else
				{
					lastPos = entry.getKey() + entry.getValue().getArg1();
					last = (String)entry.getValue().getArg2();
				}
				
				mLayoutWriter.write(String.format("%X-%X:\t\t%s\r\n", entry.getKey(), entry.getKey() + entry.getValue().getArg1() - 1, entry.getValue().getArg2()));
			}
			
			if(lastPos != file.length())
				mLayoutWriter.write(String.format("%X-%X:\t\tUnallocated space!\r\n", lastPos, file.length()));
			
			mLayoutWriter.flush();
		}
		catch(IOException e)
		{
			
		}
	}
	
	private static void logLayoutInt(GlobalReferenceFile ref)
	{
		try
		{
			File f = new File("plugins/PlayerSpy/layoutref.txt");
			if(!f.exists())
				f.getParentFile().mkdirs();
			
			if(mLayoutWriter == null)
				mLayoutWriter = new OutputStreamWriter(new FileOutputStream(f));
			
			au.com.mineauz.PlayerSpy.globalreference.HoleIndex holeIndex;
			au.com.mineauz.PlayerSpy.globalreference.ChunkIndex chunkIndex;
			GRFileHeader header;
			ACIDRandomAccessFile file;
			
			try
			{
				Field field = ref.getClass().getDeclaredField("mHoleIndex");
				field.setAccessible(true);
				
				holeIndex = (au.com.mineauz.PlayerSpy.globalreference.HoleIndex) field.get(ref);
				
				field = ref.getClass().getDeclaredField("mChunkIndex");
				field.setAccessible(true);
				
				chunkIndex = (au.com.mineauz.PlayerSpy.globalreference.ChunkIndex) field.get(ref);
				
				field = ref.getClass().getDeclaredField("mHeader");
				field.setAccessible(true);
				
				header = (GRFileHeader)field.get(ref);
				
				field = StructuredFile.class.getDeclaredField("mFile");
				field.setAccessible(true);
				
				file = (ACIDRandomAccessFile) field.get(ref);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return;
			}
			
			mLayoutWriter.write("\r\nLayout: ");
			String message = mLastMessage.get(Thread.currentThread().getId());
			if(message != null)
				mLayoutWriter.write(message);

			mLayoutWriter.write("\r\n");
			
			TreeMap<Long,Pair<Long,Object>> sortedItems = new TreeMap<Long, Pair<Long,Object>>();
			sortedItems.put(0L, new Pair<Long,Object>((long)header.getSize(),"Header"));
			
			sortedItems.put(header.FileIndexLocation, new Pair<Long,Object>(header.FileIndexSize,"File Index"));
			sortedItems.put(header.ChunkIndexLocation, new Pair<Long,Object>(header.ChunkIndexSize,"Chunk Index"));
			sortedItems.put(header.SessionIndexLocation, new Pair<Long,Object>(header.SessionIndexSize,"Session Index"));
			sortedItems.put(header.HolesIndexLocation, new Pair<Long,Object>(header.HolesIndexSize,"Holes Index (" + header.HolesIndexPadding + ")"));
			
			for(HoleEntry hole : holeIndex)
				sortedItems.put(hole.Location, new Pair<Long,Object>(hole.Size,"Hole"));
			
			for(au.com.mineauz.PlayerSpy.globalreference.ChunkEntry entry : chunkIndex)
				sortedItems.put(entry.location, new Pair<Long,Object>(entry.size,"ChunkList for " + String.format("%d, %d (%d)", entry.chunkX, entry.chunkZ, entry.worldHash)));
			
			// Find any Unallocated space
			long lastPos = 0;
			String last = "";
			for(Entry<Long, Pair<Long, Object>> entry : sortedItems.entrySet())
			{
				if(lastPos > entry.getKey())
					mLayoutWriter.write(String.format("%X-%X:\t\tCONFLICT with %s!\r\n", entry.getKey(), entry.getKey(), last));
				else if(lastPos < entry.getKey())
				{
					mLayoutWriter.write(String.format("%X-%X:\t\tUnallocated space!\r\n", lastPos, entry.getKey() - 1));
					lastPos = entry.getKey() + entry.getValue().getArg1();
					last = (String)entry.getValue().getArg2();
				}
				else
				{
					lastPos = entry.getKey() + entry.getValue().getArg1();
					last = (String)entry.getValue().getArg2();
				}
				
				mLayoutWriter.write(String.format("%X-%X:\t\t%s\r\n", entry.getKey(), entry.getKey() + entry.getValue().getArg1() - 1, entry.getValue().getArg2()));
			}
			
			if(lastPos != file.length())
				mLayoutWriter.write(String.format("%X-%X:\t\tUnallocated space!\r\n", lastPos, file.length()));
			
			mLayoutWriter.flush();
		}
		catch(IOException e)
		{
			
		}
	}
	
	private static void buildNodes(LogFile log, DefaultMutableTreeNode root)
	{
		Index<?>[] indexes = null;
		FileHeader header = null;
		try
		{
			Field field = StructuredFile.class.getDeclaredField("mIndexes");
			field.setAccessible(true);
			
			indexes = (Index<?>[]) field.get(log);
			
			field = LogFile.class.getDeclaredField("mHeader");
			field.setAccessible(true);
			
			header = (FileHeader)field.get(log);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			root.add(new DefaultMutableTreeNode("Error: " + e.getMessage()));
			return;
		}
		
		
		root.add(new DefaultMutableTreeNode("Version: " + header.VersionMajor + "." + header.VersionMinor));
		root.add(new DefaultMutableTreeNode("Owner: " + header.PlayerName));
		root.add(new DefaultMutableTreeNode("OwnerTags Required: " + header.RequiresOwnerTags));
		
		for(Index<?> index : indexes)
		{
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(index.getIndexName());
			for(int i = 0; i < index.getCount(); ++i)
			{
				IndexEntry entry = index.get(i);
				
				DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode();
				
				if(entry instanceof HoleEntry)
				{
					entryNode.setUserObject("Hole " + String.format("%X -> %X", ((HoleEntry)entry).Location, ((HoleEntry)entry).Location + ((HoleEntry)entry).Size - 1));
				}
				else if(entry instanceof OwnerMapEntry)
				{
					entryNode.setUserObject("Tag: " + ((OwnerMapEntry)entry).Id + ": " + ((OwnerMapEntry)entry).Owner);
				}
				else if(entry instanceof ChunkEntry)
				{
					ChunkEntry chunk = (ChunkEntry)entry;
					entryNode.setUserObject("Chunk List: " + chunk.listId);
					
					try
					{
						Set<CubicChunk> chunks = ((ChunkIndex)index).getChunks(chunk.listId);
						
						for(CubicChunk c : chunks)
							entryNode.add(new DefaultMutableTreeNode("Chunk: " + String.format("%d, %d, %d (%d)", c.chunkX, c.chunkY, c.chunkZ, c.worldHash)));
					}
					catch(IOException e)
					{
						entryNode.add(new DefaultMutableTreeNode("IOException: " + e.getMessage()));
					}
				}
				else if(entry instanceof SessionEntry)
				{
					String name = "Session ";
					SessionEntry session = (SessionEntry)entry;
					
					if(session.OwnerTagId == -1)
						name += session.Id;
					else
						name += log.getOwnerTag(session) + "-" + session.Id;

					if(session.Compressed)
						name += " (C)";
					
					entryNode.setUserObject(name);
					
					entryNode.add(new DefaultMutableTreeNode("ID: " + session.Id));
					entryNode.add(new DefaultMutableTreeNode("Record Count: " + session.RecordCount));
					entryNode.add(new DefaultMutableTreeNode("Compressed: " + session.Compressed));
					entryNode.add(new DefaultMutableTreeNode("OwnerTag Id: " + session.OwnerTagId));
					entryNode.add(new DefaultMutableTreeNode("ChunkList Id: " + session.ChunkListId));
					entryNode.add(new DefaultMutableTreeNode("Location: " + String.format("%X -> %X", session.Location, session.Location + session.TotalSize - 1)));
					entryNode.add(new DefaultMutableTreeNode("Padding: " + String.format("%X", session.Padding)));
					entryNode.add(new DefaultMutableTreeNode("Start Date: " + Util.dateToString(session.StartTimestamp)));
					entryNode.add(new DefaultMutableTreeNode("End Date: " + Util.dateToString(session.EndTimestamp)));
					
					DefaultMutableTreeNode recordNode = new DefaultMutableTreeNode("Records");
					
					RecordList records = log.loadSession(session);
					
					int remaining = session.RecordCount;
					
					if(records != null)
					{
						for(Record record : records)
						{
							recordNode.add(new DefaultMutableTreeNode(record));
						}
						
						remaining -= records.size();
					}

					if(remaining > 0)
						recordNode.add(new DefaultMutableTreeNode("ERROR. " + remaining + " records not read."));
					
					entryNode.add(recordNode);
				}
				else
					entryNode.setUserObject(entry);

				node.add(entryNode);
			}
			root.add(node);
		}
	
	}
	
	private static void buildNodes(GlobalReferenceFile ref, DefaultMutableTreeNode root)
	{
		Index<?>[] indexes = null;
		GRFileHeader header = null;
		try
		{
			Field field = StructuredFile.class.getDeclaredField("mIndexes");
			field.setAccessible(true);
			
			indexes = (Index<?>[]) field.get(ref);
			
			field = GlobalReferenceFile.class.getDeclaredField("mHeader");
			field.setAccessible(true);
			
			header = (GRFileHeader)field.get(ref);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			root.add(new DefaultMutableTreeNode("Error: " + e.getMessage()));
			return;
		}
		
		
		root.add(new DefaultMutableTreeNode("Version: " + header.VersionMajor + "." + header.VersionMinor));
		
		for(Index<?> index : indexes)
		{
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(index.getIndexName());
			for(int i = 0; i < index.getCount(); ++i)
			{
				IndexEntry entry = index.get(i);
				
				DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode();
				
				if(entry instanceof HoleEntry)
				{
					entryNode.setUserObject("Hole " + String.format("%X -> %X", ((HoleEntry)entry).Location, ((HoleEntry)entry).Location + ((HoleEntry)entry).Size - 1));
				}
				else if(entry instanceof FileEntry)
				{
					entryNode.setUserObject("File: " + ((FileEntry)entry).fileId.toString() + "(" + ((FileEntry)entry).fileName + ") from " + Util.dateToString(((FileEntry)entry).timeBegin) + " - " + Util.dateToString(((FileEntry)entry).timeEnd));
				}
				else if(entry instanceof au.com.mineauz.PlayerSpy.globalreference.ChunkEntry)
				{
					au.com.mineauz.PlayerSpy.globalreference.ChunkEntry chunk = (au.com.mineauz.PlayerSpy.globalreference.ChunkEntry)entry;
					
					entryNode.setUserObject("Chunk: " + String.format("%d, %d (%d)  Count: %d", chunk.chunkX, chunk.chunkZ, chunk.worldHash, chunk.count));
					
					entryNode.add(new DefaultMutableTreeNode("Location: " + String.format("%X -> %X", chunk.location, chunk.location + chunk.size - 1)));
					entryNode.add(new DefaultMutableTreeNode("Padding: " + String.format("%X", chunk.padding)));
					entryNode.add(new DefaultMutableTreeNode(""));
					
					try
					{
						Multimap<UUID, Integer> sessions = ((au.com.mineauz.PlayerSpy.globalreference.ChunkIndex)index).getSessionsInChunk(chunk.chunkX, chunk.chunkZ, chunk.worldHash);
						
						for(Entry<UUID, Integer> session : sessions.entries())
							entryNode.add(new DefaultMutableTreeNode("Session: File: " + session.getKey().toString() + " Id: " + session.getValue()));
					}
					catch(IOException e)
					{
						entryNode.add(new DefaultMutableTreeNode("IOException: " + e.getMessage()));
					}
				}
				else if(entry instanceof au.com.mineauz.PlayerSpy.globalreference.SessionEntry)
				{
					String name = "Session ";
					au.com.mineauz.PlayerSpy.globalreference.SessionEntry session = (au.com.mineauz.PlayerSpy.globalreference.SessionEntry)entry;
		
					name += session.fileId.toString() + " - " + session.sessionId;
										
					entryNode.setUserObject(name);
					
					entryNode.add(new DefaultMutableTreeNode("ID: " + session.sessionId));
					entryNode.add(new DefaultMutableTreeNode("File ID: " + session.fileId));
					entryNode.add(new DefaultMutableTreeNode("Start Date: " + Util.dateToString(session.startTime)));
					entryNode.add(new DefaultMutableTreeNode("End Date: " + Util.dateToString(session.endTime)));

				}
				else
					entryNode.setUserObject(entry);

				node.add(entryNode);
			}
			root.add(node);
		}
	}
	
	public static void showLayout(StructuredFile file)
	{
		JFrame window = new JFrame("Log Layout");
		
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(file.getFile().getName());
		
		if(file instanceof LogFile)
			buildNodes((LogFile)file, top);
		else
			buildNodes((GlobalReferenceFile)file, top);
		
		JTree tree = new JTree(top);
		JScrollPane treeView = new JScrollPane(tree);
		
		window.getContentPane().add(treeView);
		
		window.pack();
		window.setVisible(true);
		window.setSize(500, 500);
		
	}
	
	public static void startSection()
	{
		mVariables.put(Thread.currentThread().getId(), new HashMap<String, Object>());
	}
	public static void stopSection()
	{
		mVariables.remove(Thread.currentThread().getId());
	}
	
	public static void recordVariable(String name, Object data)
	{
		HashMap<String, Object> varList = mVariables.get(Thread.currentThread().getId());
		if(varList == null)
			return;
		
		varList.put(name, data);
	}
	
	public static Map<String, Object> getVariables()
	{
		return mVariables.get(Thread.currentThread().getId());
	}
}
