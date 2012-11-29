package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;

import au.com.mineauz.PlayerSpy.Cause;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;
import au.com.mineauz.PlayerSpy.search.interfaces.Modifier;

public class SearchFilter 
{
	public ArrayList<Constraint> orConstraints = new ArrayList<Constraint>();
	public ArrayList<Constraint> andConstraints = new ArrayList<Constraint>();
	public ArrayList<Cause> causes = new ArrayList<Cause>();
	public ArrayList<Modifier> modifiers = new ArrayList<Modifier>();
	
	public boolean noLimit = false;
	
	public String getDescription()
	{
		String or = "";
		String and = "";
		String cause = "";

		for(int i = 0; i < orConstraints.size(); i++)
		{
			if(i != 0)
				or += " or ";
			or += orConstraints.get(i).getDescription();
		}
		
		for(int i = 0; i < andConstraints.size(); i++)
		{
			if(i != 0)
				and += " and ";
			and += andConstraints.get(i).getDescription();
		}
		
		for(int i = 0; i < causes.size(); i++)
		{
			if(i != 0)
				cause += " or ";
			cause += causes.get(i).friendlyName();
		}
		
		String result = "";
		
		if(!or.isEmpty())
			result += "( " + or + " )";
		if(!and.isEmpty())
		{
			if(!result.isEmpty())
				result += " and ";
			result += "( " + and + " )";
		}
		
		if(!cause.isEmpty())
		{
			if(!result.isEmpty())
				result += " by ";
			result += cause;
		}
		
		return result;
	}
}
