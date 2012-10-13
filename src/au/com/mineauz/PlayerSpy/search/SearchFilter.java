package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayList;

import au.com.mineauz.PlayerSpy.Cause;

public class SearchFilter 
{
	public ArrayList<Constraint> orConstraints = new ArrayList<Constraint>();
	public ArrayList<Constraint> andConstraints = new ArrayList<Constraint>();
	public ArrayList<Cause> causes = new ArrayList<Cause>(); 
}
