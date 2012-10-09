package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import org.bukkit.OfflinePlayer;

import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class PlayerConstraintDA extends DataAssembler 
{

	@Override
	public Object assemble(ArrayDeque<Object> objects) 
	{
		OfflinePlayer player = (OfflinePlayer)objects.pop();
		objects.pop();
		
		PlayerConstraint constraint = new PlayerConstraint();
		constraint.player = player;
		
		return constraint;
	}

}
