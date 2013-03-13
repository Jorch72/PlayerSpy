package au.com.mineauz.PlayerSpy.Utilities;

public class StringUtil
{
	public static int getNextNonSpaceChar(String string, int fromPos)
	{
		while(fromPos < string.length() && Character.isSpaceChar(string.charAt(fromPos)))
			++fromPos;
		
		return fromPos;
	}
	public static int getNextNonDigitChar(String string, int fromPos)
	{
		while(fromPos < string.length() && Character.isDigit(string.charAt(fromPos)))
			++fromPos;
		
		return fromPos;
	}
	
	public static boolean startsWithIgnoreCase(String string, String toMatch, int fromPos)
	{
		return string.toLowerCase().startsWith(toMatch.toLowerCase(), fromPos);
	}
	
	public static void validateExpected(String string, int pos, String expected, String errorMsg)
	{
		if(pos > string.length()-expected.length())
			throw new IllegalArgumentException(errorMsg);
		if(!string.startsWith(expected, pos))
			throw new IllegalArgumentException(errorMsg);
	}
}
