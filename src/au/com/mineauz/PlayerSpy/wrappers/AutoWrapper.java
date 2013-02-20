package au.com.mineauz.PlayerSpy.wrappers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class AutoWrapper
{
	private static HashMap<Class<? extends AutoWrapper>, Class<?>> mClasses = new HashMap<Class<? extends AutoWrapper>, Class<?>>();
	private static HashMap<Class<?>,Class<? extends AutoWrapper>> mClassReverse = new HashMap<Class<?>,Class<? extends AutoWrapper>>();
	
	private Object mInstance;
	
	protected AutoWrapper()
	{
		initialize(getClass());
	}
	
	@SuppressWarnings( "unchecked" )
	private static Class<?> convert(Class<?> other)
	{
		if(AutoWrapper.class.isAssignableFrom(other))
			return getWrappedClass((Class<? extends AutoWrapper>)other);
		else if(other.equals(Void.class))
			return Void.TYPE;
		else if(other.equals(Byte.class))
			return Byte.TYPE;
		else if(other.equals(Short.class))
			return Short.TYPE;
		else if(other.equals(Integer.class))
			return Integer.TYPE;
		else if(other.equals(Long.class))
			return Long.TYPE;
		else if(other.equals(Float.class))
			return Float.TYPE;
		else if(other.equals(Double.class))
			return Double.TYPE;
		
		return other;
	}
	/**
	 * This is only needed if a class has static methods that you need to use. You should put it in the static constructor
	 */
	protected static void initialize(Class<? extends AutoWrapper> thisClass)
	{
		if(getWrappedClass(thisClass) != null)
			return;
		
		findClass(thisClass);
		
		// Validate the class
		Class<?> clazz = getWrappedClass(thisClass);
		
		try
		{
			// Validate and map methods
			for(Field field : thisClass.getDeclaredFields())
			{
				WrapperMethod annotation = field.getAnnotation(WrapperMethod.class);
				
				if(annotation != null)
				{
					// Swap out this class for the wrapped class
					Class<?> retType = convert(annotation.returnType());

					Class<?>[] parTypes = annotation.parameterTypes();
					for(int i = 0; i < parTypes.length; ++i)
						parTypes[i] = convert(parTypes[i]);
					
					validateMethod(thisClass, annotation.name(), retType, parTypes);
					
					Method method = clazz.getDeclaredMethod(annotation.name(), parTypes);
					field.setAccessible(true);
					field.set(null, method);
				}
			}
			
			// Validate and map constructors
			for(Field field : thisClass.getDeclaredFields())
			{
				WrapperConstructor annotation = field.getAnnotation(WrapperConstructor.class);
				
				if(annotation != null)
				{
					// Swap out this class for the wrapped class
					Class<?>[] parTypes = annotation.value();
					for(int i = 0; i < parTypes.length; ++i)
						parTypes[i] = convert(parTypes[i]);
					
					validateConstructor(thisClass, parTypes);
					
					Constructor<?> constructor = clazz.getDeclaredConstructor(parTypes);
					field.setAccessible(true);
					field.set(null, constructor);
				}
			}
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected static Class<?> getWrappedClass(Class<? extends AutoWrapper> thisClass)
	{
		return mClasses.get(thisClass);
	}
	
	protected void instanciate(Constructor<?> constructor, Object... args)
	{
		try
		{
			mInstance = constructor.newInstance(args);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected static AutoWrapper instanciateWrapper(Object obj)
	{
		try
		{
			if(mClassReverse.containsKey(obj))
			{
				Class<? extends AutoWrapper> wrapperClass = mClassReverse.get(obj);
				
				AutoWrapper wrapper = wrapperClass.newInstance();
				wrapper.mInstance = obj;
				
				return wrapper;
			}
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
		return null;
	}
	
	protected static void validateConstructor(Class<? extends AutoWrapper> thisClass, Class<?>... argTypes)
	{
		Class<?> clazz = getWrappedClass(thisClass);
		assert(clazz != null);
		
		try
		{
			clazz.getDeclaredConstructor(argTypes);
		}
		catch(Exception e)
		{
			throw new WrapperValidationException("Wrapper integrity validation failed: constructor " + argTypes.toString() + " expected in " + clazz.getName() + " is not available.");
		}
	}
	
	protected static void validateMethod(Class<? extends AutoWrapper> thisClass, String name, Class<?> returnType, Class<?>... argTypes)
	{
		Class<?> clazz = getWrappedClass(thisClass);
		assert(clazz != null);
		
		try
		{
			Method method = clazz.getDeclaredMethod(name, argTypes);
			if(!method.getReturnType().equals(returnType))
				throw new WrapperValidationException("Wrapper integrity validation failed: Method " + name + " expected in " + clazz.getName() + " has a signature difference.");
			
		}
		catch(WrapperValidationException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new WrapperValidationException("Wrapper integrity validation failed: Method " + name + " expected in " + clazz.getName() + " is not available.");
		}
		
	}
	
	protected static void validateField(Class<? extends AutoWrapper> thisClass, String name, Class<?> fieldType)
	{
		Class<?> clazz = getWrappedClass(thisClass);
		assert(clazz != null);
		
		try
		{
			Field field = clazz.getDeclaredField(name);
			if(!field.getType().equals(fieldType))
				throw new WrapperValidationException("Wrapper integrity validation failed: Field " + name + " expected in " + clazz.getName() + " has a signature difference.");
			
		}
		catch(WrapperValidationException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new WrapperValidationException("Wrapper integrity validation failed: Field " + name + " expected in " + clazz.getName() + " is not available.");
		}
	}
	
	@SuppressWarnings( "unchecked" )
	private static <T> T callMethod(Object instance, Method method, Object... args)
	{
		try
		{
			// Unwrap objects
			for(int i = 0; i < args.length; ++i)
			{
				if(args[i] instanceof AutoWrapper)
					args[i] = ((AutoWrapper)args[i]).mInstance;
			}
			return (T)method.invoke(instance, args);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	protected static <T> T callStaticMethod(Method method, Object... args)
	{
		return callMethod(null, method, args);
	}
	
	protected <T> T callMethod(Method method, Object... args)
	{
		assert mInstance != null;
		
		return callMethod(mInstance, method, args);
	}
	
	@SuppressWarnings( "unchecked" )
	protected <T> T getFieldInstance(String fieldName)
	{
		Class<?> clazz = getWrappedClass(getClass());
		assert(clazz != null);
		
		try
		{
			Field field = clazz.getDeclaredField("fieldName");
			return (T) field.get(mInstance);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static void findClass(Class<? extends AutoWrapper> thisClass) 
	{
		if (mClasses.containsKey(thisClass))
			return;
		
		// Get the annotations
		WrapperClass annotation = thisClass.getAnnotation(WrapperClass.class);
		
		if(annotation == null)
			throw new IllegalStateException("All AutoWrappers must have a WrapperClass annotation!");
		
		String desiredClassName = annotation.value();
		Class<?> foundClass = null;
		
		try
		{
			// Find and load the class the wrapper is for
			
			String[] parts = desiredClassName.split("\\.");
			
			int startIndex = 0;
			String startPath = "";
			for(startIndex = 0; startIndex < parts.length; ++startIndex)
			{
				if(parts[startIndex].equals("*"))
					break;
				
				if(startIndex != 0)
					startPath += "/";
				
				startPath += parts[startIndex];
			}
			
			String classPath = walkPath(startPath,parts,startIndex);
			
			if(classPath == null)
				throw new RuntimeException("Error wrapping '" + desiredClassName + "'. No matching classes were found.");
			
			foundClass = Class.forName(classPath.replaceAll("/", "."));
			mClassReverse.put(foundClass, thisClass);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Error wrapping '" + desiredClassName + "'. No matching classes were found.");
		}
		finally
		{
			mClasses.put(thisClass, foundClass);
		}
	}
	
	private static String walkPath(String currentPath, String[] neededParts, int index) throws IOException, URISyntaxException
	{
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        
        
        
        String[] next = getNextParts(currentPath);
        
        for(String part : next)
    	{
        	if(neededParts[index].equals("*") || neededParts[index].equals(part))
        	{
        		if(index + 1 == neededParts.length)
                	return currentPath + "/" + part;
        		
        		String path = walkPath(currentPath + "/" + part, neededParts, index+1);
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

        packageURL = classLoader.getResource(path);

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
