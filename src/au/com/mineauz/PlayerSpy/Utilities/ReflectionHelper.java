package au.com.mineauz.PlayerSpy.Utilities;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ReflectionHelper
{
	/**
	 * Does the same as Class.forName, but supports the wildchar 
	 * @throws ClassNotFoundException *
	 */
	public static Class<?> forName(String path) throws ClassNotFoundException
	{
		try
		{
			String[] parts = path.split("\\.");
			
			int startIndex = 0;
			String startPath = "";
			for(startIndex = 0; startIndex < parts.length; ++startIndex)
			{
				if(parts[startIndex].equals("*"))
					break;
				
				if(startIndex != 0)
					startPath += ".";
				
				startPath += parts[startIndex];
			}
			
			String classPath = walkPath(startPath,parts,startIndex);
			if (classPath == null)
				throw new ClassNotFoundException("Cannot find class " + path);
			
			return Class.forName(classPath);
		}
		catch(URISyntaxException e)
		{
			throw new ClassNotFoundException("Cannot find class " + path);
		}
		catch ( IOException e )
		{
			throw new ClassNotFoundException("Cannot find class " + path);
		}
	}
	
	
	private static String walkPath(String currentPath, String[] neededParts, int index) throws IOException, URISyntaxException
	{
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        
        
        String[] next = getNextParts(currentPath);
        
        String needed = neededParts[index];
        if(needed.contains("$"))
        	needed = needed.substring(0, needed.indexOf("$"));
        
        for(String part : next)
    	{
        	
        	if(needed.equals("*") || needed.equals(part))
        	{
        		if(index + 1 == neededParts.length)
                	return currentPath + "." + part;
        		
        		String path = walkPath(currentPath + "." + part, neededParts, index+1);
        		if(path != null)
        			return path;
        	}
    	}
        
        return null;
	}
	
	private static String[] getNextParts(String path) throws IOException, URISyntaxException
	{
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        
        URL packageURL;
        HashSet<String> foundNames = new HashSet<String>();

        packageURL = classLoader.getResource(path.replaceAll("\\.", "/"));

        if(packageURL.getProtocol().equals("jar"))
        {
            String jarFileName;
            JarFile jf;
            Enumeration<JarEntry> jarEntries;
            String entryName;

            // build jar file name, then loop through zipped entries
            jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
            jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));
            jf = new JarFile(jarFileName);
            jarEntries = jf.entries();
            
            while(jarEntries.hasMoreElements())
            {
                entryName = jarEntries.nextElement().getName();
                
                if(entryName.startsWith(path + "/"))
                {
                	entryName = entryName.substring(path.length()+1);

                	if(entryName.contains("/"))
                		entryName = entryName.substring(0, entryName.indexOf('/'));
                	
                	if(entryName.contains("."))
                		entryName = entryName.substring(0, entryName.indexOf('.'));
                	
                	if(!entryName.isEmpty())
                		foundNames.add(entryName);
                }
            }
            
            jf.close();

        // loop through files in classpath
        }
        else
        {
            File folder = new File(packageURL.getFile());
            File[] contenuti = folder.listFiles();
            String entryName;
            for(File actual: contenuti)
            {
                entryName = actual.getName();
                entryName = entryName.substring(0, entryName.lastIndexOf('.'));
                foundNames.add(entryName);
            }
        }
        
        return foundNames.toArray(new String[foundNames.size()]);
    }
}
