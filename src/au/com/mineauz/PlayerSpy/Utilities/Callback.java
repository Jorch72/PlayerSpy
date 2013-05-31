package au.com.mineauz.PlayerSpy.Utilities;

public interface Callback<T>
{
	public void onSuccess(T data);
	
	public void onFailure(Throwable error);
}
