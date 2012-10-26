package au.com.mineauz.PlayerSpy.search;

import java.util.ArrayDeque;

import au.com.mineauz.PlayerSpy.fsa.DataAssembler;

public class GenericDA extends DataAssembler
{
	private int mPopCount;
	private Object mToInsert;
	
	public GenericDA(Object toInsert, int removeCount)
	{
		mToInsert = toInsert;
		mPopCount = removeCount;
	}
	
	@Override
	public Object assemble( ArrayDeque<Object> objects )
	{
		for(int i = 0; i < mPopCount; i++)
			objects.pop();
		
		return mToInsert;
	}

}
