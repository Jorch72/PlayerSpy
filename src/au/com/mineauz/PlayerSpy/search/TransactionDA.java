package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class TransactionDA extends DataAssembler
{

	@SuppressWarnings( "unchecked" )
	@Override
	public Object assemble( ArrayDeque<Object> objects )
	{
		Object temp = objects.pop();
		
		TransactionConstraint constraint = new TransactionConstraint();
		
		if(temp instanceof OfflinePlayer || temp instanceof Short || temp instanceof Pair<?,?> && (((String)objects.peekFirst()).equalsIgnoreCase("from") || ((String)objects.peekFirst()).equalsIgnoreCase("to")))
		{
			objects.pop();
			constraint.mTargetFilter = temp;
			temp = objects.pop();
		}
		
		if(temp instanceof Integer) // Amount
		{
			constraint.mAmount = (Integer)temp;
			temp = objects.pop();
		}
		
		constraint.mFilterType = (Pair<Material,Integer>)temp;
		
		constraint.mTook = ((String)objects.pop()).equalsIgnoreCase("take");
		
		return constraint;
	}

}
