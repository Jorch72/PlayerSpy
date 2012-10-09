package au.com.mineauz.PlayerSpy.fsa;

import java.util.ArrayDeque;

import org.bukkit.Material;

import au.com.mineauz.PlayerSpy.Pair;
import au.com.mineauz.PlayerSpy.Util;

public class BlockIdState extends State
{

	@Override
	public boolean match(String word, ArrayDeque<Object> output) 
	{
		String idString, metaString;
		
		if(word.contains(":"))
		{
			idString = word.split(":")[0];
			metaString = word.split(":")[1];
		}
		else
		{
			idString = word;
			metaString = null;
		}
		
		Material mat = Util.parseBlock(idString);
		
		if(mat == null)
			// Try to parse by integer id
		{
			try
			{
				int id = Integer.parseInt(idString);
				mat = Material.getMaterial(id);
			}
			catch(NumberFormatException e)
			{
				return false;
			}
		}
		
		if(mat == null || !mat.isBlock())
			// Could not find a match
			return false;
		
		int meta = -1;
		// Try to parse meta
		if(metaString != null)
		{
			try
			{
				meta = Integer.parseInt(metaString);
			}
			catch(NumberFormatException e)
			{
				return false;
			}
		}
		
		output.push(new Pair<Material, Integer>(mat, meta));
		
		return true;
	}

	@Override
	public String getExpected() 
	{
		return "block id or name";
	}

}
