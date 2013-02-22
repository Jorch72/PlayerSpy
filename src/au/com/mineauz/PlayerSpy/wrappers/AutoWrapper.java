package au.com.mineauz.PlayerSpy.wrappers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import au.com.mineauz.PlayerSpy.Utilities.ReflectionHelper;

public abstract class AutoWrapper
{
	private static HashMap<Class<? extends AutoWrapper>, Class<?>> mClasses = new HashMap<Class<? extends AutoWrapper>, Class<?>>();
	private static HashMap<Class<?>,Class<? extends AutoWrapper>> mClassReverse = new HashMap<Class<?>,Class<? extends AutoWrapper>>();
	
	protected Object mInstance;
	
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
	
	static Object wrapObjects(Object obj)
	{
		if(obj == null)
			return null;
		
		if(obj instanceof List<?>)
		{
			// Make safe all the items
			List<Object> copy = new ArrayList<Object>(((List<?>)obj).size());
			
			for(int i = 0; i < copy.size(); ++i)
				copy.add(wrapObjects(((List<?>)obj).get(i)));
			
			return copy;
		}
		else if(mClassReverse.containsKey(obj.getClass()))
		{
			return instanciateWrapper(obj);
		}
		else if(obj.getClass().isArray())
		{
			// Make safe all the items
			Object[] copy = ((Object[])obj).clone();
			
			for(int i = 0; i < copy.length; ++i)
				copy[i] = wrapObjects(copy[i]);
			
			return copy;
		}
		else
			return obj;
	}
	
	static Object unwrapObjects(Object obj)
	{
		if(obj == null)
			return null;
		
		if(obj instanceof List<?>)
		{
			// Make safe all the items
			List<Object> copy = new ArrayList<Object>(((List<?>)obj).size());
			
			for(int i = 0; i < copy.size(); ++i)
				copy.add(unwrapObjects(((List<?>)obj).get(i)));
			
			return copy;
		}
		else if(obj instanceof AutoWrapper)
		{
			return ((AutoWrapper)obj).mInstance;
		}
		else if(obj.getClass().isArray())
		{
			// Make safe all the items
			Object[] copy = ((Object[])obj).clone();
			
			for(int i = 0; i < copy.length; ++i)
				copy[i] = unwrapObjects(copy[i]);
			
			return copy;
		}
		else
			return obj;
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
			
			// Validate and map fields
			for(Field field : thisClass.getDeclaredFields())
			{
				WrapperField annotation = field.getAnnotation(WrapperField.class);
				
				if(annotation != null)
				{
					// Swap out this class for the wrapped class
					Class<?> type = convert(annotation.type());
					
					validateField(thisClass, annotation.name(), type);
					
					if(Modifier.isStatic(field.getModifiers()))
					{
						Field wrappedField = clazz.getDeclaredField(annotation.name());
						FieldWrapper<?> wrapper = new FieldWrapper<Object>(wrappedField, null);
						
						field.setAccessible(true);
						field.set(null, wrapper);
					}
				}
			}
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void initializeFields()
	{
		try
		{
			Class<?> clazz = getWrappedClass(getClass());
			
			// Validate and map fields
			for(Field field : getClass().getDeclaredFields())
			{
				WrapperField annotation = field.getAnnotation(WrapperField.class);
				
				if(annotation != null)
				{
					Field wrappedField = clazz.getDeclaredField(annotation.name());
					FieldWrapper<?> wrapper = new FieldWrapper<Object>(wrappedField, mInstance);
					
					field.setAccessible(true);
					field.set(mInstance, wrapper);
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
			
			initializeFields();
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected void instanciate()
	{
		try
		{
			mInstance = mClasses.get(getClass()).newInstance();
			
			initializeFields();
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected static AutoWrapper instanciateWrapper(Object obj)
	{
		if(obj == null)
			return null;
		
		try
		{
			if(mClassReverse.containsKey(obj.getClass()))
			{
				Class<? extends AutoWrapper> wrapperClass = mClassReverse.get(obj.getClass());
				
				Constructor<? extends AutoWrapper> c = wrapperClass.getDeclaredConstructor();
				c.setAccessible(true);
				
				AutoWrapper wrapper = c.newInstance();
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
			if(args != null)
			{
				for(int i = 0; i < args.length; ++i)
					args[i] = unwrapObjects(args[i]);
			}
			return (T)wrapObjects(method.invoke(instance, args));
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
			Field field = clazz.getDeclaredField(fieldName);
			return (T) wrapObjects(field.get(mInstance));
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected void setFieldInstance(String fieldName, Object value)
	{
		Class<?> clazz = getWrappedClass(getClass());
		assert(clazz != null);
		
		try
		{
			Field field = clazz.getDeclaredField(fieldName);
			field.set(mInstance, unwrapObjects(value));
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
			foundClass = ReflectionHelper.forName(desiredClassName);

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
}
