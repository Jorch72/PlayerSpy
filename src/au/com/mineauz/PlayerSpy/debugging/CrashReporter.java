package au.com.mineauz.PlayerSpy.debugging;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrashReporter
{
	private Throwable mException;
	private String mMessage;
	
	private HashMap<String, Object> mVariables = new HashMap<String, Object>();
	
	public CrashReporter(Throwable exception)
	{
		mException = exception;
	}
	
	public void setMessage(String message)
	{
		mMessage = message;
	}
	
	public void addVariable(String name, Object value)
	{
		mVariables.put(name, value);
	}
	
	public void addVariables(Map<String, Object> variables)
	{
		if(variables == null)
			return;
		mVariables.putAll(variables);
	}
	
	public void log(Logger logger)
	{
		logger.severe("An Error Occured: " + (mMessage != null ? mMessage : ""));
		logger.log(Level.SEVERE, "", mException);
		
		logger.severe("Variables: ");
		for(Entry<String,Object> variable : mVariables.entrySet())
			logger.severe(" - " + variable.getKey() + ": " + (variable.getValue() == null ? "null" : variable.getValue().toString())); 
	}

}
