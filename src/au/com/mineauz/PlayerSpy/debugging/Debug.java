package au.com.mineauz.PlayerSpy.debugging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.LogUtil;

public class Debug 
{
	private static Logger mDebugLog;
	private static File mDebugLogFile;
	private static Handler mHandler;
	
	private static HashMap<Player, Level> mDebugReceiver = new HashMap<Player, Level>();
	
	public static synchronized void init(File debugLogFile)
	{
		mDebugLogFile = debugLogFile;
		try
		{
			OutputStream stream = new FileOutputStream(debugLogFile, true);

			mDebugLog = Logger.getLogger("PlayerSpyDebug");
			mDebugLog.setLevel(Level.FINEST);
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
		if(mDebugLog == null)
			return;
		mDebugLog.log(Level.SEVERE, "Caught exception", e);
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
		
		String sourceClass = null;
		String sourceMethod = null;
		
		for(int i = 0; i < stack.length; ++i)
		{
			sourceClass = stack[i].getClassName();
			
			if(sourceClass.equals(Debug.class.getName()))
				continue;
			
			sourceMethod = stack[i].getMethodName();
			break;
		}
		
		mDebugLog.logp(level, sourceClass, sourceMethod, message);
	}
}
