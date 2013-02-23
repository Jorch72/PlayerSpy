package au.com.mineauz.PlayerSpy.debugging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class DebugLogFormatter extends Formatter
{
	SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yy hh:mm:ss a");
	
	@Override
	public String format( LogRecord record )
	{
		// Find the thread that was used
		Thread theThread = null;
		
		for(Thread t : Thread.getAllStackTraces().keySet())
			if (t.getId() == record.getThreadID())
			{
				theThread = t;
				break;
			}
		
		String result = String.format("%s [%d-%s] [%s] \t",fmt.format(new Date(record.getMillis())), record.getThreadID(), (theThread != null ? theThread.getName() : "Unknown Thread"), record.getLevel().toString().toUpperCase());
		
		if (record.getMessage() != null)
			result += '"' + record.getMessage() + '"';
		
		if(record.getThrown() != null)
		{
			result += "\n    ";
			Throwable throwable = record.getThrown();
			while (throwable != null)
			{
				result += throwable.getClass().getName() + ": " + throwable.getMessage() + "\n";
				StackTraceElement[] stack = throwable.getStackTrace();
				
				for(StackTraceElement frame : stack)
				{
					String src = "";
					if(frame.getFileName() == null)
						src = "Unknown Source";
					else
					{
						src = frame.getFileName() + ":";
						
						if(frame.getLineNumber() < 0)
							src += "?";
						else
							src += frame.getLineNumber() + "";
					}
					
					result += String.format("    at %s(%s)\n", frame.getClassName() + "." + frame.getMethodName(), src);
				}
				
				throwable = throwable.getCause();
				
				if(throwable != null)
					result += "    Caused by: ";
			}
			result += "   ";
		}
		
		if (record.getSourceClassName() != null)
			result += " called from " + record.getSourceClassName() + ":" + record.getSourceMethodName();
		
		return result + "\n";
	}

	
}
