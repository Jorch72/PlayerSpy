package au.com.mineauz.PlayerSpy;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class FLLTest 
{
	private RandomAccessFile mFile;
	private TestFLL mTest;
	
	public FLLTest()
	{
	}
	
	
	public void doTest()
	{
		try
		{
			LogUtil.info("Beginning test of FileLinkedList");
			File path = new File("test.list");
			if(path.exists())
				path.delete();
			
			mFile = new RandomAccessFile(path, "rw");
//			mTest = new TestFLL(mFile, 0, false);
			//LogUtil.info("There are " + mTest.size() + " items in the list");
			mTest = new TestFLL(mFile, 0, true);
			
			int count = 25;
			for(int trial = 0; trial < 100; trial++)
			{
				LogUtil.info("Begining trial " + trial);
				boolean random = true;
				long time = 0;
				if(random)
				{
					Random r = new Random(trial);
					ArrayList<Integer> sequence = new ArrayList<Integer>(count);
					for(int i = 0; i < count; i++)
						sequence.add(i);
		
					// Now randomise them
					for(int i = 0; i < count; i++)
					{
						// Swap this with 1 random one
						int swapInd = r.nextInt(count);
						if(swapInd == i)
							continue;
						
						// Swap them
						int other = sequence.get(swapInd);
						sequence.set(swapInd,sequence.get(i));
						sequence.set(i, other);
					}
					
					LogUtil.info("*Inserting " + count + " items randomly into the list");
					time = Calendar.getInstance().getTimeInMillis();
					
					int i = 0;
					for(i = 0; i < count; i++)
					{
						if(!mTest.add(new TestElement(sequence.get(i))))
							break;
					}
				
					if(i == count)
						LogUtil.info("*All items have been inserted");
					else
						LogUtil.severe("*Some of the items failed to insert");
				}
				else
				{
					LogUtil.info("*Inserting " + count + " items sequentially into the list");
					time = Calendar.getInstance().getTimeInMillis();
					
					int i = 0;
					for(i = 0; i < count; i++)
					{
						if(!mTest.add(new TestElement(i)))
							break;
					}
					
					if(i == count)
						LogUtil.info("*All items have been inserted");
					else
						LogUtil.severe("*Some of the items failed to insert");
				}
				
				LogUtil.info("Operation took " + ((Calendar.getInstance().getTimeInMillis() - time) / 1000F) + " seconds to complete");
				LogUtil.info("Shifts: " + mTest.Debug);
				mTest.Debug = 0;
				
				LogUtil.info("Now removing all items");
				
				random = true;
				if(random)
				{
					Random r = new Random(trial + 10000);
					
					LogUtil.info("* Removing " + count + " items randomly from the list");
					time = Calendar.getInstance().getTimeInMillis();
					
					int i = 0;
					for(i = 0; i < count; i++)
					{
						if(mTest.remove((int)r.nextInt(mTest.size())) == null)
							break;
					}
				
					if(i == count)
						LogUtil.info("*All items have been removed");
					else
						LogUtil.severe("*Some of the items failed to remove");
				}
				else
				{
					LogUtil.info("*Removing " + count + " items sequentially from the list");
					time = Calendar.getInstance().getTimeInMillis();
					
					int i = 0;
					for(i = 0; i < count; i++)
					{
						if(mTest.remove(0) == null)
							break;
					}
					
					if(i == count)
						LogUtil.info("*All items have been remove");
					else
						LogUtil.severe("*Some of the items failed to be removed");
				}
				LogUtil.info("Operation took " + ((Calendar.getInstance().getTimeInMillis() - time) / 1000F) + " seconds to complete");
			}
			
			mFile.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	public void doTest2()
	{
		try
		{
			LogUtil.info("Beginning test 2 of FileLinkedList");
			File path = new File("test.list");
			if(path.exists())
				path.delete();
			
			mFile = new RandomAccessFile(path, "rw");
			mTest = new TestFLL(mFile, 0, true);
			
			Random r = new Random(1532);
			long time = Calendar.getInstance().getTimeInMillis();
			for(int trial = 0; trial < 1000; trial++)
			{

				if(r.nextBoolean() || mTest.size() == 0)
				{
					// Insert
					mTest.add(new TestElement(trial));
				}
				else
				{
					// Remove
					mTest.remove((int)r.nextInt(mTest.size()));
				}
			}
			LogUtil.info("Operation took " + ((Calendar.getInstance().getTimeInMillis() - time) / 1000F) + " seconds to complete");
			
			mFile.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	class TestFLL extends FileLinkedList<TestElement>
	{

		public TestFLL(RandomAccessFile file, long location, boolean isNew) throws IOException 
		{
			super(file, location, TestElement.class, isNew);
		}

		@Override
		protected long onRequestSpace(long size) 
		{
			try {
				return mFile.length();
			} catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
		}

		@Override
		protected void onUseSpace(long location, long size) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void onReliquishSpace(long location, long size) {
			// TODO Auto-generated method stub
			
		}
		
	}
}


class TestElement implements IWritable
{
	public TestElement()
	{
		
	}
	public TestElement(int number)
	{
		Number = number;
	}
	public int Number;
	
	@Override
	public int hashCode() 
	{
		return Number;
	}
	@Override
	public void write(RandomAccessFile file) throws IOException 
	{
		file.writeInt(Number);
	}

	@Override
	public void read(RandomAccessFile file) throws IOException 
	{
		Number = file.readInt();
	}

	@Override
	public int getSize() 
	{
		return 4;
	}
	
}