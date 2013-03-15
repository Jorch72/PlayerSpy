package au.com.mineauz.PlayerSpy.attributes;

import java.util.HashMap;
import java.util.List;

import au.com.mineauz.PlayerSpy.Utilities.StringUtil;
import au.com.mineauz.PlayerSpy.search.ReverserFormatter;
import au.com.mineauz.PlayerSpy.search.ShowLocationModifier;
import au.com.mineauz.PlayerSpy.search.interfaces.Modifier;

public class OptionsAttribute extends NamedAttribute
{
	private static HashMap<String, Modifier> mOptions;
	
	static
	{
		mOptions = new HashMap<String, Modifier>();
		
		Modifier m = new ShowLocationModifier();
		mOptions.put("show location", m);
		mOptions.put("showlocation", m);
		mOptions.put("show-location", m);
		
		mOptions.put("reverse", new ReverserFormatter());
	}
	
	public OptionsAttribute()
	{
		super("options",AttributeValueType.Set, "option");
	}
	
	@Override
	protected int parseElement( String input, int start, List<Object> output ) throws IllegalArgumentException
	{
		String bestMatch = "";
		for(String name : mOptions.keySet())
		{
			if(name.length() > bestMatch.length() && StringUtil.startsWithIgnoreCase(input, name, start))
				bestMatch = name;
		}
		
		if(bestMatch.isEmpty())
			throw new IllegalArgumentException("Unknown option starting at '" + (start + 10 <= input.length() ? input.substring(start, start+10) + "...'" : input.substring(start) + "'"));
		
		output.add(mOptions.get(bestMatch));
		return start + bestMatch.length();
	}

}
