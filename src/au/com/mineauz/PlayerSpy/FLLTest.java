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
			TestFLL test = new TestFLL(mFile, 0, true);
			TestFLL test2 = new TestFLL(mFile, mFile.length(), true);
			TestFLL test3 = new TestFLL(mFile, mFile.length(), true);
			Random orderer = new Random(12345);
			
			int count = 25;
			for(int trial = 0; trial < 100; trial++)
			{
				LogUtil.info("Begining trial " + trial);
				boolean random = true;
				long time = 0;
				if(random)
				{
					Random r = new Random(trial * 91 + 5);
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
						int v = orderer.nextInt(3);
						LogUtil.info("Adding into " + v);
						if(v == 0)
						{
							if(!test.add(new TestElement(sequence.get(i))))
								break;
						}
						else if(v == 1)
						{
							if(!test2.add(new TestElement(sequence.get(i))))
								break;
						}
						else
						{
							if(!test3.add(new TestElement(sequence.get(i))))
								break;
						}
						
						if(orderer.nextInt(10) == 0)
						{
							// Potentially force a duplicate
							i--;
							continue;
						}
						
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
						int v = orderer.nextInt(3);
						LogUtil.info("Adding into " + v);
						if(v == 0)
						{
							if(!test.add(new TestElement(i)))
								break;
						}
						else if(v == 1)
						{
							if(!test2.add(new TestElement(i)))
								break;
						}
						else
						{
							if(!test3.add(new TestElement(i)))
								break;
						}
					}
					
					if(i == count)
						LogUtil.info("*All items have been inserted");
					else
						LogUtil.severe("*Some of the items failed to insert");
				}
				
				LogUtil.info("Operation took " + ((Calendar.getInstance().getTimeInMillis() - time) / 1000F) + " seconds to complete");
				LogUtil.info("Shifts: " + test.Debug);
				test.Debug = 0;
				
				LogUtil.info("Now removing all items");
				
				random = true;
				if(random)
				{
					Random r = new Random((trial + 10000) * 2);
					
					LogUtil.info("* Removing " + count + " items randomly from the list");
					time = Calendar.getInstance().getTimeInMillis();
				
					LogUtil.info("Removing from 1");
					while(test.size() > 0)
					{
						if(test.remove((int)r.nextInt(test.size())) == null)
							break;
					}
					LogUtil.info("Removing from 2");
					while(test2.size() > 0)
					{
						if(test2.remove((int)r.nextInt(test2.size())) == null)
							break;
					}
					LogUtil.info("Removing from 3");
					while(test3.size() > 0)
					{
						if(test3.remove((int)r.nextInt(test3.size())) == null)
							break;
					}
					
					if(test.size() != 0)
						LogUtil.severe("test1 still has " + test.size() + " items in it");
					if(test2.size() != 0)
						LogUtil.severe("test2 still has " + test2.size() + " items in it");
					if(test3.size() != 0)
						LogUtil.severe("test3 still has " + test3.size() + " items in it");
				}
				else
				{
					LogUtil.info("*Removing " + count + " items sequentially from the list");
					time = Calendar.getInstance().getTimeInMillis();
					
					int i = 0;
					for(i = 0; i < count; i++)
					{
						if(test.remove(0) == null)
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
			TestFLL test = new TestFLL(mFile, 0, true);
			
			Random r = new Random(1532);
			long time = Calendar.getInstance().getTimeInMillis();
			for(int trial = 0; trial < 1000; trial++)
			{

				if(r.nextBoolean() || test.size() == 0)
				{
					// Insert
					test.add(new TestElement(trial));
				}
				else
				{
					// Remove
					test.remove((int)r.nextInt(test.size()));
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