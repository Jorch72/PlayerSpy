package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import org.bukkit.Material;

import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class AnyBlockDA extends DataAssembler 
{
	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		objects.pop();
		objects.pop();
		return new Pair<Material, Integer>(Material.AIR, -1);
	}

}
