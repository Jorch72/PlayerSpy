package au.com.mineauz.PlayerSpy.debugging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.bukkit.ChatColor;

public class ChatOutputFormatter extends Formatter
{
	@Override
	public String format( LogRecord record )
	{
		String result = ChatColor.GOLD + record.getLevel().toString() + ChatColor.WHITE + " ";
		
		if (record.getMessage() != null)
			result += record.getMessage();
		
		if(record.getThrown() != null)
		{
			result += "\n ";
			Throwable throwable = record.getThrown();
			int results = 0;
			int maxResults = 3;
			
			while (throwable != null)
			{
				results = 0;
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
					
					result += String.format(" at %s(%s)", frame.getClassName() + "." + frame.getMethodName(), src);
					results++;
					
					if(results >= maxResults)
						break;
				}
				
				throwable = throwable.getCause();
				
				if(throwable != null)
					result += "\n Caused by: ";
			}
		}
	
		return result;
	}

	
}
