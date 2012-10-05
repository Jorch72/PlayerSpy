package au.com.mineauz.PlayerSpy;

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
}
