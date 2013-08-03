package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;

import au.com.mineauz.PlayerSpy.search.interfaces.CauseConstraint;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;
import au.com.mineauz.PlayerSpy.search.interfaces.Modifier;

public class SearchFilter 
{
	public ArrayList<Constraint> andConstraints = new ArrayList<Constraint>();
	public ArrayList<CauseConstraint> causes = new ArrayList<CauseConstraint>();
	public ArrayList<Modifier> modifiers = new ArrayList<Modifier>();
	
	public boolean noLimit = false;
	
	public String getDescription()
	{
		String and = "";
		String cause = "";
		
		int andCount = 0;

		for(int i = 0; i < andConstraints.size(); i++)
		{
			String str = andConstraints.get(i).getDescription();
			if(str != null)
			{
				if(!and.isEmpty())
					and += " and ";
				
				and += str;
				andCount++;
			}
		}
		
		for(int i = 0; i < causes.size(); i++)
		{
			String str = causes.get(i).getDescription();
			if(str != null)
			{
				if(!cause.isEmpty())
					cause += " and ";
				
				cause += str;
			}
		}
		
		String result = "";
		
		if(!and.isEmpty())
		{
			if(!result.isEmpty())
				result += " and ";
			
			if(andCount > 1)
				result += "( " + and + " )";
			else
				result += and;
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
