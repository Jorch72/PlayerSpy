package au.com.mineauz.PlayerSpy.Utilities;


public interface IMatchable
{
	public Match matchNext(String input, int start) throws IllegalArgumentException;
}
