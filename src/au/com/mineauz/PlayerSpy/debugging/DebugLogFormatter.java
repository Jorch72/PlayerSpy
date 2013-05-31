package au.com.mineauz.PlayerSpy.debugging;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.bukkit.craftbukkit.libs.joptsimple.internal.Strings;

public class DebugLogFormatter extends Formatter
{
	SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yy hh:mm:ss a");
	
	private HashMap<Integer, StackTraceElement[]> mPreviousStacks = new HashMap<Integer, StackTraceElement[]>();
	private HashMap<Integer, Integer> mPreviousLevel = new HashMap<Integer, Integer>();
	
	private long mLastTime = 0;
	private int mLastThread = 0;
	
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
		
		// Build the flow trace
		StackTraceElement[] stack = null;
		
		if(record.getParameters() != null && record.getParameters().length > 0)
			stack = (StackTraceElement[])record.getParameters()[0];
		
		int currentLevel = 0;
		if(mPreviousLevel.containsKey(record.getThreadID()))
			currentLevel = mPreviousLevel.get(record.getThreadID());
		
		boolean changed = false;
		
		String flow = "";
		
		if(stack != null)
		{
			StackTraceElement[] oldStack = mPreviousStacks.get(record.getThreadID());
			
			StackTraceElement[] entering = null;
			StackTraceElement[] exiting = null;
			
			if(oldStack != null)
			{
				// find the last common element
				int i;
				for(i = 1; i <= Math.min(oldStack.length, stack.length); ++i)
				{
					if(!oldStack[oldStack.length-i].equals(stack[stack.length-i]))
						break;
				}
				
				if(i <= stack.length)
					entering = Arrays.copyOfRange(stack, 0, stack.length-i);
				if(i <= oldStack.length)
					exiting = Arrays.copyOfRange(oldStack, 0, oldStack.length-i);
			}
			else
			{
				entering = stack;
			}
			
			if(exiting != null && exiting.length != 0)
			{
				for(int i = 0; i < exiting.length; ++i)
				{
					String src = "";
					if(exiting[i].getFileName() == null)
						src = "Unknown Source";
					else
					{
						src = exiting[i].getFileName() + ":";
						
						if(exiting[i].getLineNumber() < 0)
							src += "?";
						else
							src += exiting[i].getLineNumber() + "";
					}
					
					--currentLevel;
					flow += Strings.repeat(' ', currentLevel*2);
					
					flow += "< " + String.format("%s(%s)%n", exiting[i].getClassName() + "." + exiting[i].getMethodName(), src);
					changed = true;
				}
			}
			
			if(entering != null && entering.length != 0)
			{
				for(int i = entering.length-1; i >= 0; --i)
				{
					String src = "";
					if(entering[i].getFileName() == null)
						src = "Unknown Source";
					else
					{
						src = entering[i].getFileName() + ":";
						
						if(entering[i].getLineNumber() < 0)
							src += "?";
						else
							src += entering[i].getLineNumber() + "";
					}
					
					flow += Strings.repeat(' ', currentLevel*2);
					++currentLevel;
					flow += "> " + String.format("%s(%s)%n", entering[i].getClassName() + "." + entering[i].getMethodName(), src);
					changed = true;
				}
			}
			
			mPreviousStacks.put(record.getThreadID(), stack);
			mPreviousLevel.put(record.getThreadID(), currentLevel);
		}
		
		String result = "";
		
		if(mLastThread == record.getThreadID() && record.getMillis() - mLastTime < 1000)
		{
			if(changed)
				result += "\n" + flow + "\n";
		}
		else
		{
			result = String.format("\n%s [%d-%s] [%s]%n================================================%n",fmt.format(new Date(record.getMillis())), record.getThreadID(), (theThread != null ? theThread.getName() : "Unknown Thread"), record.getLevel().toString().toUpperCase());
			
			if(changed)
				result += flow + "\n";
		}
		
		if (record.getMessage() != null)
			result += Strings.repeat(' ', currentLevel*2) + record.getMessage();
		
		if(record.getThrown() != null)
		{
			result += "\n    ";
			Throwable throwable = record.getThrown();
			while (throwable != null)
			{
				result += Strings.repeat(' ', currentLevel*2) + throwable.getClass().getName() + ": " + throwable.getMessage() + "\n";
				StackTraceElement[] trace = throwable.getStackTrace();
				
				for(StackTraceElement frame : trace)
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
					
					result += Strings.repeat(' ', currentLevel*2) + String.format("    at %s(%s)%n", frame.getClassName() + "." + frame.getMethodName(), src);
				}
				
				throwable = throwable.getCause();
				
				if(throwable != null)
					result += Strings.repeat(' ', currentLevel*2) + "    Caused by: ";
			}
		}
		
		mLastTime = record.getMillis();
		mLastThread = record.getThreadID();
		
		return result + "\n";
	}

	
}
