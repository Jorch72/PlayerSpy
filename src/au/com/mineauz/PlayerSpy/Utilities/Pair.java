package au.com.mineauz.PlayerSpy.Utilities;

public class Pair<A,B> 
{
	private A mArg1;
	private B mArg2;
	
	public Pair(A arg1, B arg2)
	{
		mArg1 = arg1;
		mArg2 = arg2;
	}
	
	public A getArg1()
	{
		return mArg1;
	}
	
	public B getArg2()
	{
		return mArg2;
	}

	public void setArg1(A arg1) 
	{
		mArg1 = arg1;
	}
	public void setArg2(B arg2) 
	{
		mArg2 = arg2;
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof Pair))
			return false;
		
		Pair<?,?> p = (Pair<?,?>)obj;
		
		if(!((mArg1 == null && p.getArg1() == null) || (mArg1 != null && mArg1.equals(p.getArg1()))))
			return false;
		
		if(!((mArg2 == null && p.getArg2() == null) || (mArg2 != null && mArg2.equals(p.getArg2()))))
			return false;
		
		return true;
	}
	
	@Override
	public int hashCode()
	{
		int hash = 17;
		if(mArg1 != null)
			hash ^= mArg1.hashCode() * 13;
		if(mArg2 != null)
			hash ^= mArg2.hashCode() * 11;
		
		return hash;
	}
	@Override
	public String toString() 
	{
		return "Pair: (" + mArg1 + ", " + mArg2 + " )";
	}
}
