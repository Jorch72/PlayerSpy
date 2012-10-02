package au.com.mineauz.PlayerSpy;

import java.util.logging.Logger;

public class LogUtil 
{
	private static Logger mLogger;
	
	public static synchronized void setLogger(Logger logger)
	{
		mLogger = logger;
	}
	
	public static synchronized void info(String text)
	{
		if(mLogger != null)
			mLogger.info(text);
	}
	
	public static synchronized void warning(String text)
	{
		if(mLogger != null)
			mLogger.warning(text);
	}
	
	public static synchronized void severe(String text)
	{
		if(mLogger != null)
			mLogger.severe(text);
	}
	
	public static synchronized void fine(String text)
	{
		if(mLogger != null)
			mLogger.fine(text);
	}
	
	public static synchronized void finer(String text)
	{
		if(mLogger != null)
			mLogger.finer(text);
	}
	
	public static synchronized void finest(String text)
	{
		if(mLogger != null)
			mLogger.finest(text);
	}
}
