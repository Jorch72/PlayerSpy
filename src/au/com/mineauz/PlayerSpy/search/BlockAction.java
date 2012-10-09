package au.com.mineauz.PlayerSpy.search;

import org.bukkit.Material;

import au.com.mineauz.PlayerSpy.Pair;

public class BlockAction extends Action 
{
	public boolean placed;
	public Pair<Material, Integer> material;
	
	@Override
	public String toString() 
	{
		return "{ placed: " + placed + ", material: " + material + "}"; 
	}
}
