package au.com.mineauz.PlayerSpy.Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class StringTranslator
{
	private static HashMap<String, String> sCurrentStringTable;
	
	public static boolean setActiveLanguage(String lang)
	{
		InputStream stream = ClassLoader.getSystemResourceAsStream("assets/minecraft/lang/" + lang + ".lang");
		
		if(stream == null)
			return false;
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		
		sCurrentStringTable = new HashMap<String, String>();
		try
		{
			String line = reader.readLine(); 
			while(line != null)
			{
				if(!line.isEmpty() && !line.trim().startsWith("#"))
				{
					if(line.contains("="))
					{
						String key = line.substring(0,line.indexOf("="));
						String value = line.substring(line.indexOf("=")+1);
						sCurrentStringTable.put(key, value);
					}
				}	
				line = reader.readLine();
			}
			stream.close();
			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
		
	}
	public static String translateString(String key)
	{
		String result = sCurrentStringTable.get(key); 
		return (result != null ? result : "");
	}
	public static String translateName(String key)
	{
		String result = sCurrentStringTable.get(key + ".name"); 
		return (result != null ? result : "");
	}
	
	public static HashMap<String, String> getStringTable()
	{
		return sCurrentStringTable;
	}
}
