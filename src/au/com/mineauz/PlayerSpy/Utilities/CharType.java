package au.com.mineauz.PlayerSpy.Utilities;

public enum CharType
{
	Letter,
	Symbol,
	Digit,
	Whitespace;
	
	public static CharType get(char ch)
	{
		if(Character.isDigit(ch))
			return Digit;
		else if(Character.isLetter(ch))
			return Letter;
		else if(Character.isSpaceChar(ch))
			return Whitespace;
		else
			return Symbol;
	}
	
	/**
	 * Checks if these 2 chartypes can be part of the same word
	 */
	public boolean isJoined(CharType other)
	{
		if(other == this)
			return true;
		
		if((this == Letter || this == Digit) && (other == Letter || other == Digit))
			return true;
		
		return false;
	}
}
