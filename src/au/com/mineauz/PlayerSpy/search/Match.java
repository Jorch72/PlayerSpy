package au.com.mineauz.PlayerSpy.search;

public class Match
{
	public final int startPosition;
	public final int endPosition;
	public final Object value;
	public final Object matchingSource;
	
	public Match(int start, int end, Object value, Object source)
	{
		startPosition = start;
		endPosition = end;
		this.value = value;
		matchingSource = source;
	}
}
