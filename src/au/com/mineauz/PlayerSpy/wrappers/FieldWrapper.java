package au.com.mineauz.PlayerSpy.wrappers;

import java.lang.reflect.Field;

public class FieldWrapper<T>
{
	private Field mField;
	private Object mInstance;
	
	public FieldWrapper(Field field, Object instance)
	{
		mField = field;
		mField.setAccessible(true);
		mInstance = instance;
	}
	
	@SuppressWarnings( "unchecked" )
	public T get()
	{
		try
		{
			return (T) AutoWrapper.wrapObjects(mField.get(mInstance));
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void set(T value)
	{
		try
		{
			mField.set(mInstance, value);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
