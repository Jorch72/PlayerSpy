package au.com.mineauz.PlayerSpy;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public abstract class FileHashSet<E extends IWritable> implements Set<E> 
{
	public class FileIterator implements Iterator<E>
	{
		private int mIndex;
		private FileIterator()
		{
			mIndex = 0;
		}
		@Override
		public boolean hasNext() 
		{
			return mIndex < mCount;
		}

		@Override
		public E next() 
		{
			if(mIndex >= mCount)
				throw new IndexOutOfBoundsException();
			
			try
			{
				@SuppressWarnings("unchecked")
				E item = (E)mTemplateClass.newInstance();
				
				mFile.seek(mLocation + mIndex * mItemSize);
				item.read(mFile);
				
				mIndex++;
				return item;
			}
			catch(IOException e)
			{
				e.printStackTrace();
			} 
			catch (InstantiationException e) 
			{
				e.printStackTrace();
			} 
			catch (IllegalAccessException e) 
			{
				e.printStackTrace();
			}
			
			return null;
		}

		@Override
		public void remove() 
		{
			// Nope.avi
		}
		
	}
	
	private Class<? extends IWritable> mTemplateClass;
	private RandomAccessFile mFile;
	protected long mLocation;
	protected long mSize;
	private int mCount;
	private int mItemSize;
	
	public FileHashSet(RandomAccessFile file, long location, long size, int count, int itemSize, Class<? extends IWritable> templateClass)
	{
		mFile = file;
		mLocation = location;
		mSize = size;
		mCount = count;
		mItemSize = itemSize;
		mTemplateClass = templateClass;
	}
	
	@Override
	public int size() 
	{
		return mCount;
	}

	@Override
	public boolean isEmpty() 
	{
		return mCount != 0;
	}

	@Override
	public boolean contains(Object o) 
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterator<E> iterator() 
	{
		return new FileIterator();
	}

	@Override
	public Object[] toArray() 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean add(E item) 
	{
		try
		{
			long oldSize = mSize;
			if(onExpandRequest(mItemSize))
			{
				// We can expand
				int insertPos = 0;
				if(mCount != 0)
				{
					// Do a binary search for the insert point
					int upper = mCount-1;
					int lower = 0;
					IWritable temp = item.getClass().newInstance();
					int hash = item.hashCode();
					
					while(upper > lower)
					{
						int mid = (lower + upper) / 2;
						// Get the date
						mFile.seek(mLocation + mid * mItemSize);
						temp.read(mFile);
						
						if(hash > temp.hashCode())
							lower = mid;
						else
							upper = mid + 1;
					}
					insertPos = lower;
				}
				
				// Insert it
				byte[] buffer = new byte[mItemSize];
				for(int i = mCount - 1; i > insertPos ; i--)
				{
					// Read the existing one
					mFile.seek(mLocation + i * mItemSize);
					mFile.readFully(buffer);
					
					// Move it
					mFile.seek(mLocation + (i+1) * mItemSize);
					mFile.write(buffer);
				}
				
				// Write the new entry
				mFile.seek(mLocation + insertPos * mItemSize);
				item.write(mFile);
				
				onInserted(item, insertPos);
				
				mSize = mFile.getFilePointer() - mLocation;
				mCount++;
				onExpandComplete(oldSize);
			}
			else
			{
				long newLocation = onRelocateRequest((mItemSize + 1) * mCount);
				
				IWritable temp = item.getClass().newInstance();
				int hash = item.hashCode();
				
				boolean coppied = false;
				// Copy it across and insert the entry while youre at it
				for(int i = 0, j = 0; i < mCount; i++, j++)
				{
					// Read the old
					mFile.seek(mLocation + i * mItemSize);
					temp.read(mFile);
					
					mFile.seek(newLocation + j * mItemSize);
					// Insert in the new entry
					if(!coppied && hash < temp.hashCode())
					{
						onInserted(item, i);
						item.write(mFile);
						j++; // Allows j == i before insert, and j == i+1 after
						coppied = true;
					}
					// Write it back in the new location
					temp.write(mFile);
				}
				
				if(!coppied)
				{
					// Append it
					item.write(mFile);
					onInserted(item, mCount);
				}
				
				long oldLocation = mLocation;
				mLocation = newLocation;
				mSize = mFile.getFilePointer() - mLocation;
				mCount++;
				
				onRelocateComplete(oldLocation, oldSize);
			}
			
			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		} 
		catch (InstantiationException e) 
		{
			e.printStackTrace();
			return false;
		} 
		catch (IllegalAccessException e) 
		{
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean remove(Object o) 
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) 
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) 
	{
		for(E item : c)
		{
			if(!add(item))
				return false;
		}
		return true;
	}

	@Override
	public boolean retainAll(Collection<?> c) 
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) 
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() 
	{
		// TODO Auto-generated method stub
		
	}

	protected abstract long onRelocateRequest(long size) throws IOException;
	protected abstract void onRelocateComplete(long oldLocation, long oldSize) throws IOException;
	
	protected abstract boolean onExpandRequest(long size) throws IOException;
	protected abstract void onExpandComplete(long oldSize) throws IOException;
	
	protected abstract void onContract(long oldSize) throws IOException;
	
	protected abstract void onInserted(IWritable item, int location) throws IOException;
}
