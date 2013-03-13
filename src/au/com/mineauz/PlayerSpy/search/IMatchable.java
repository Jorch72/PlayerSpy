package au.com.mineauz.PlayerSpy.search;

public interface IMatchable
{
	public Match matchNext(String input, int start) throws IllegalArgumentException;
}
