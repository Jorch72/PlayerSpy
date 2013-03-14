package au.com.mineauz.PlayerSpy.search.interfaces;

public interface IConstraint<T>
{
	public boolean matches(T item);
	
	public String getDescription();
}
