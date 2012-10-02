package au.com.mineauz.PlayerSpy;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public abstract class FileLinkedList<E extends IWritable> implements List<E>
{
	private RandomAccessFile mFile;
	private long mLocation;
	private ListHeader mHeader;
	private Class<? extends IWritable> mItemClass;
	private HashMap<Long, Long> mAddressMappings = new HashMap<Long, Long>();
	
	public int Debug = 0;
	public class FLLIterator implements Iterator<E>
	{
		long location = 0;
		public FLLIterator()
		{
			location = mHeader.LeftElement;
		}
		
		@Override
		public boolean hasNext() 
		{
			return (location != 0);
		}

		@Override
		public E next() 
		{
			try
			{
				if(location != 0)
				{
					ItemContainer<E> container = readAt(location);
					location = container.NextPointers[0];
					return container.Data;
				}
				return null;
			}
			catch(IOException e)
			{
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public void remove() 
		{
			// TODO: Remove
		}
		
	}
	
	public FileLinkedList(RandomAccessFile file, long location, Class<? extends IWritable> itemClass, boolean isNew) throws IOException
	{
		mFile = file;
		mLocation = location;
		mHeader = new ListHeader();
		mItemClass = itemClass;
		
		if(isNew)
		{
			mLocation = onRequestSpace(ListHeader.cSize);
			mHeader.LeftElement = 0;
			mHeader.RightElement = 0;
			mHeader.TopElement = 0;
			mHeader.ItemCount = 0;
			mHeader.LevelCount = 0;
			
			mFile.seek(mLocation);
			mHeader.write(mFile);
			
			onUseSpace(mLocation, ListHeader.cSize);
		}
		else
		{
			mFile.seek(mLocation);
			mHeader.read(mFile);
		}
	}

	@Override
	public int size() 
	{
		return mHeader.ItemCount;
	}

	@Override
	public boolean isEmpty() 
	{
		return mHeader.ItemCount == 0;
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
		return new FLLIterator();
	}

	@Override
	public Object[] toArray() 
	{
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) 
	{
		return null;
	}

	@Override
	public boolean add(E item) 
	{
		try
		{
			LogUtil.info("** Adding item with hash: " + item.hashCode());
			
			ItemContainer<E> newContainer = new ItemContainer<E>();
			newContainer.Data = item;
			
			// Find the insert location (hash based)
			int hash = item.hashCode();
			
			int level = mHeader.LevelCount - 1;
			
			// Read the top item
			long location = mHeader.TopElement;
			int index = 0;
			
			ItemContainer<E> container;
			
			if(mHeader.ItemCount != 0)
			{
				container = readAt(mHeader.TopElement);
				index += container.LastWidths[container.Levels-1];
			}
			else
			{
				container = new ItemContainer<E>();
				mHeader.LevelCount = 1;
				level = -1;
			}
			
			int dir = 0;
			int insertDir = -1;
			while(level >= 0)
			{
				if(hash < container.Data.hashCode())
				{
					if(dir != 1)
					{
						// Go left
						if(container.LastPointers[level] == 0)
						{
							level--;
							insertDir = -1;
							dir = 0;
						}
						else
						{
							location = container.LastPointers[level];
							index -= container.LastWidths[level];
							
							container = readAt(container.LastPointers[level]);
							Debug++;
							dir = -1;
						}
					}
					else
					{
						insertDir = -1;
						level--;
						dir = 0;
					}
				}
				else if(hash > container.Data.hashCode())
				{
					if(dir != -1)
					{
						// Go Right
						if(container.NextPointers[level] == 0)
						{
							level--;
							insertDir = 1;
							dir = 0;
						}
						else
						{
							location = container.NextPointers[level];
							index += container.NextWidths[level];
							
							container = readAt(container.NextPointers[level]);
							Debug++;
							dir = 1;
						}
					}
					else
					{
						insertDir = 1;
						level--;
						dir = 0;
					}
				}
				else
				{
					// Go down
					level--;
					insertDir = -1;
					dir = 0;
				}
			}
			if(insertDir == 1)
				index++;
			
			newContainer.Levels = 1;
			newContainer.LastPointers = new long[1];
			newContainer.LastWidths = new int[1];
			newContainer.NextPointers = new long[1];
			newContainer.NextWidths = new int[1];
			
			long newLocation = onRequestSpace(newContainer.getSize());
			
			if(mHeader.ItemCount != 0)
			{
				if(insertDir == -1) // to the left
				{
					newContainer.LastPointers[0] = container.LastPointers[0];
					newContainer.LastWidths[0] = container.LastWidths[0];
					
					newContainer.NextPointers[0] = location;
					newContainer.NextWidths[0] = 1;
					
					container.LastPointers[0] = newLocation;
					container.LastWidths[0] = 1;
				}
				else // To the right
				{
					newContainer.NextPointers[0] = container.NextPointers[0];
					newContainer.NextWidths[0] = container.NextWidths[0];
					
					newContainer.LastPointers[0] = location;
					newContainer.LastWidths[0] = 1;
					
					container.NextPointers[0] = newLocation;
					container.NextWidths[0] = 1;
				}
				
				// Update existing
				writeAt(location, container);
				
				// Update the other one
				if(insertDir == -1 && newContainer.LastPointers[0] != 0)
				{
					container = readAt(newContainer.LastPointers[0]);
					container.NextPointers[0] = newLocation;
					container.NextWidths[0] = 1;
					writeAt(newContainer.LastPointers[0], container);
				}
				else if(insertDir == 1 && newContainer.NextPointers[0] != 0)
				{
					container = readAt(newContainer.NextPointers[0]);
					container.LastPointers[0] = newLocation;
					container.LastWidths[0] = 1;
					writeAt(newContainer.NextPointers[0], container);
				}
			}
			else
			{
				newContainer.LastPointers[0] = 0;
				newContainer.LastWidths[0] = 0;
				
				newContainer.NextPointers[0] = 0;
				newContainer.NextWidths[0] = 0;
			}
			// Write new
			writeAt(newLocation, newContainer);
			onUseSpace(newLocation, newContainer.getSize());
			
			// Update the widths of all ones above, that pass over the inserted node
			if(newContainer.Levels != mHeader.LevelCount)
				updateWidths(index, newContainer, true);
			
			// Update the header
			mHeader.ItemCount++;
			
			if(index == 0)
				mHeader.LeftElement = newLocation;
			if(index == mHeader.ItemCount-1)
				mHeader.RightElement = newLocation;
			
			if(mHeader.LevelCount == 1 && mHeader.TopElement == 0)
			{
				mHeader.TopElement = newLocation;
			}
			
			handlePromote(newContainer, newLocation, 0, index);
			clearModifiedAddresses();
			mFile.seek(mLocation);
			mHeader.write(mFile);

			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	private void updateWidths(int index, ItemContainer<E> container, boolean insert) throws IOException
	{
		// Go above this until we get to the root increasing the last counts by one
		ItemContainer<E> temp = container;
		long tempLocation = 0;
		
		int dir = 1;
		int curIndex = index;
		// Going to the right
		for(int i = container.Levels; i < mHeader.LevelCount; i++)
		{
			// Go accross until we can go up
			boolean hasChangedDir = false;
			dir = 1;
			while(temp.Levels == i)
			{
				// Get the next node in the chain
				if(dir == 1)
					tempLocation = getModifiedAddress(temp.NextPointers[i-1]);
				else
					tempLocation = getModifiedAddress(temp.LastPointers[i-1]);
				
				// Cant go accross
				if(tempLocation == 0)
				{
					if(!hasChangedDir)
					{
						//  Change Dirs
						dir *= -1;
						hasChangedDir = true;
						continue;
					}
					else
						return; // Nothing more to do
				}
				
				if(dir == 1)
					curIndex += temp.NextWidths[i-1] - (!insert && temp == container ? 1 : 0);
				else
					curIndex -= temp.LastWidths[i-1] + (!insert && temp == container ? 1 : 0);
				
				temp = readAt(tempLocation);
			}
			

			
			ItemContainer<E> neighbour;
			if(((insert && curIndex > index) || (!insert && curIndex >= index)) && curIndex - temp.LastWidths[i] + (insert ? -1 : 1) <= index)
				// This node has a link that passes over the node
			{
				if(temp.LastPointers[i] != 0)
				{
					// Adjust the width on the other side too
					neighbour = readAt(temp.LastPointers[i]);
					try
					{
					if(insert)
						neighbour.NextWidths[i]++;
					else
						neighbour.NextWidths[i]--;
					
					writeAt(temp.LastPointers[i], neighbour);
					}
					catch(ArrayIndexOutOfBoundsException e)
					{
						LogUtil.severe("Item " + neighbour.Data.hashCode() + " @" + temp.LastPointers[i] + " has " + neighbour.Levels + " but should have at least " + (i + 1));
						throw e;
					}
				}
				
				if(insert)
					temp.LastWidths[i]++;
				else
					temp.LastWidths[i]--;
				writeAt(tempLocation,temp);
			}
			else if(((insert && curIndex < index) || (!insert && curIndex <= index)) && curIndex + temp.NextWidths[i] + (insert ? 1 : -1) >= index)
				// This node has a link that passes over the node
			{
				if(temp.NextPointers[i] != 0)
				{
					// Adjust the width on the other side too
					neighbour = readAt(temp.NextPointers[i]);
					try
					{
					if(insert)
						neighbour.LastWidths[i]++;
					else
						neighbour.LastWidths[i]--;
					writeAt(temp.NextPointers[i], neighbour);
					}
					catch(ArrayIndexOutOfBoundsException e)
					{
						LogUtil.severe("Item " + neighbour.Data.hashCode() + " @" + temp.LastPointers[i] + " has " + neighbour.Levels + " but should have at least " + (i + 1));
						throw e;
					}
				}
				
				if(insert)
					temp.NextWidths[i]++;
				else
					temp.NextWidths[i]--;
				writeAt(tempLocation,temp);
			}
		}
	}
	private ItemContainer<E> readAt(long location) throws IOException
	{
		if(location == 0)
			return null;
		mFile.seek(location);
		ItemContainer<E> container = new ItemContainer<E>();
		container.read(mFile, mItemClass);
		
		return container;
	}
	private void writeAt(long location, ItemContainer<E> container) throws IOException
	{
		mFile.seek(location);
		container.write(mFile);
	}
	private long relocate(ItemContainer<E> container, long currentLocation, int lastSize) throws IOException
	{
		onReliquishSpace(currentLocation, lastSize);
		
		long location = onRequestSpace(container.getSize());
		writeAt(location, container);
		onUseSpace(location,container.getSize());
		
		LogUtil.info("*** Relocating item " + container.Data.hashCode() + " from @" + currentLocation + " to @" + location);
		mAddressMappings.put(currentLocation, location);
		
		// Update the references
		for(int level = 0; level < container.Levels; level++)
		{
			if(container.LastPointers[level] != 0)
			{
				ItemContainer<E> neighbour = readAt(container.LastPointers[level]);
				neighbour.NextPointers[level] = location;
				writeAt(container.LastPointers[level], neighbour);
			}
			
			if(container.NextPointers[level] != 0)
			{
				ItemContainer<E> neighbour = readAt(container.NextPointers[level]);
				neighbour.LastPointers[level] = location;
				writeAt(container.NextPointers[level], neighbour);
			}
		}
		
		return location;
	}
	private void findAndApplyNeighbourAt(ItemContainer<E> container, long address, int level, int dir, int index) throws IOException
	{
		long addr = (dir == -1 ? container.LastPointers[level-1] : container.NextPointers[level-1]);
		int dist = (dir == -1 ? container.LastWidths[level-1] : container.NextWidths[level-1]);
		
		while(addr != 0)
		{
			ItemContainer<E> neighbour = readAt(addr);
			if(neighbour.Levels - 1 >= level)
			{
				if(dir == -1)
				{
					container.LastPointers[level] = addr;
					container.LastWidths[level] = dist;
					
					
					if(neighbour.NextPointers[level] != 0)
					{
						container.NextPointers[level] = neighbour.NextPointers[level];
						container.NextWidths[level] = neighbour.NextWidths[level] - dist;
					}
					neighbour.NextPointers[level] = address;
					neighbour.NextWidths[level] = dist;
					
					// Update other neighbour
					if(container.NextPointers[level] != 0)
					{
						ItemContainer<E> other = readAt(container.NextPointers[level]);
						other.LastPointers[level] = address;
						other.LastWidths[level] = container.NextWidths[level];
						writeAt(container.NextPointers[level], other);
					}
				}
				else
				{
					container.NextPointers[level] = addr;
					container.NextWidths[level] = dist;
					
					if(neighbour.LastPointers[level] != 0)
					{
						container.LastPointers[level] = neighbour.LastPointers[level];
						container.LastWidths[level] = neighbour.LastWidths[level] - dist;
					}
					neighbour.LastPointers[level] = address;
					neighbour.LastWidths[level] = dist;
					
					// Update other neighbour
					if(container.LastPointers[level] != 0)
					{
						ItemContainer<E> other = readAt(container.LastPointers[level]);
						other.NextPointers[level] = address;
						other.NextWidths[level] = container.LastWidths[level];
						writeAt(container.LastPointers[level], other);
					}
				}
				
				// Update the modified neighbour
				writeAt(addr, neighbour);
				break;
			}
			else
			{
				dist += (dir == -1 ? neighbour.LastWidths[level-1] : neighbour.NextWidths[level-1]);
				addr = (dir == -1 ? neighbour.LastPointers[level-1] : neighbour.NextPointers[level-1]);
			}
		}
		
		// Update the side links
		if(container.LastPointers[level] == 0)
			container.LastWidths[level] = index;
		
		if(container.NextPointers[level] == 0)
			container.NextWidths[level] = mHeader.ItemCount - index - 1;
	}
	private boolean handlePromote(ItemContainer<E> container, long location, int level, int index) throws IOException
	{
		// PROMOTION RULES:
		// 1. if your adjacent neighbours are the same level as you, you get promoted. else, goto step 4
		// 2. Do step 1 on your 2 new neighbours
		// 3. Repeat Step 1
		// 4. End
		
		ItemContainer<E> left = null, right = null;
		
		// Get the left item
		if(container.LastPointers[level] != 0)
			left = readAt(container.LastPointers[level]);
		// Get the right item
		if(container.NextPointers[level] != 0)
			right = readAt(container.NextPointers[level]);
		
		// Dont promote if we are the only one at this level
		if(left == null && right == null)
			return false;
		
		if((left == null || left.Levels == container.Levels) && (right == null || right.Levels == container.Levels))
		{
			level++;
			LogUtil.info("*** Promoting item " + container.Data.hashCode() + " to level " + level);
			int size = container.getSize();
			// Promote
			container.promote();
			
			location = relocate(container, location, size);
			
			if(level >= mHeader.LevelCount)
			{
				mHeader.LevelCount = level+1;
				mHeader.TopElement = location;
				LogUtil.info("*** Item " + container.Data.hashCode() + " is now top element");
				
				try
				{
					mFile.seek(mLocation);
					mHeader.write(mFile);
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
			// Got to connect up the nodes
			findAndApplyNeighbourAt(container,location, level, -1, index);
			if(container.NextPointers[level] == 0)
				findAndApplyNeighbourAt(container,location, level, 1, index);
			
			// Apply changes
			writeAt(location, container);
			
			// Retry promote
			if(!handlePromote(container, location, level, index))
			{
				// Cascade promote
				if(container.LastPointers[level] != 0)
					handlePromote(readAt(container.LastPointers[level]), container.LastPointers[level], level, index - container.LastWidths[level]);
				if(container.NextPointers[level] != 0)
					handlePromote(readAt(container.NextPointers[level]), container.NextPointers[level], level, index + container.NextWidths[level]);
			}
			return true;
		}
		
		return false;
	}
	
	/**
	 * Demotes an item back to level 0
	 */
	private void doTotalDemote(ItemContainer<E> container, long location, int index) throws IOException
	{
		long[] lastPointers = container.LastPointers;
		long[] nextPointers = container.NextPointers;
		int[] lastWidths = container.LastWidths;
		int[] nextWidths = container.NextWidths;
		
		LogUtil.info("** Demoting item " + container.Data.hashCode() + " back to level 0 @" + location);
		ItemContainer<E> topLeft = null, topRight = null;
		long topRightLocation = 0;
		
		if(location == mHeader.TopElement) // This is the top element
		{
			LogUtil.info("Transfering top status to neighbour");
			// Find someone else to become the top
			for(int l = container.Levels-1; l >= 1; l--)
			{
				if(lastPointers[l] != 0)
				{
					mHeader.TopElement = lastPointers[l];
					break;
				}
				if(nextPointers[l] != 0)
				{
					mHeader.TopElement = nextPointers[l];
					break;
				}
			}
		}
		// Reconnect all neighbours jumping over this one
		for(int l = container.Levels-1; l >= 1; l--)
		{
			ItemContainer<E> left,right;
			left = readAt(lastPointers[l]);
			right = readAt(nextPointers[l]);
			
			if(l == container.Levels-1)
			{
				topLeft = left;
				topRight = right;
				topRightLocation = nextPointers[l];
			}
			// Reconnect the nodes
			if(left == null && right == null)
				continue;
			
			if(right != null)
			{
				right.LastWidths[l] += lastWidths[l];
				right.LastPointers[l] = lastPointers[l];
				
				writeAt(nextPointers[l], right);
			}
			
			if(left != null)
			{
				left.NextWidths[l] += nextWidths[l];
				left.NextPointers[l] = nextPointers[l];
				
				writeAt(lastPointers[l], left);
			}
		}
		
		// Demote it
		int oldSize = container.getSize();
		container.LastPointers = Arrays.copyOf(container.LastPointers, 1);
		container.NextPointers = Arrays.copyOf(container.NextPointers, 1);
		container.LastWidths = Arrays.copyOf(container.LastWidths, 1);
		container.NextWidths = Arrays.copyOf(container.NextWidths, 1);
		
		container.Levels = 1;
		
		onReliquishSpace(location + container.getSize(), oldSize - container.getSize());
		writeAt(location, container);
		
		// try to promote/demote neighbours
		if(topLeft != null)
		{
			if(!handlePromote(topLeft,lastPointers[lastPointers.length-1], topLeft.Levels-1, index - lastWidths[lastWidths.length-1]))
				handleDemote(topLeft,lastPointers[lastPointers.length-1], topLeft.Levels-1, index - lastWidths[lastWidths.length-1]);
		}
		// Because the promotion / demotions of the top left can affect the top right, we will need to reload it
		topRight = readAt(getModifiedAddress(topRightLocation));
		if(topRight != null)
		{
			if(!handlePromote(topRight,getModifiedAddress(topRightLocation), topRight.Levels-1, index + nextWidths[nextWidths.length-1]))
				handleDemote(topRight,getModifiedAddress(topRightLocation), topRight.Levels-1, index + nextWidths[nextWidths.length-1]);
		}
	}
	private boolean transferLevelsToNeighbour(ItemContainer<E> container, long location, int index) throws IOException
	{
		ItemContainer<E> left,right;
		
		left = readAt(container.LastPointers[0]);
		right = readAt(container.NextPointers[0]);
		
		
		if(left == null && right == null)
			return false;
		
		
		ItemContainer<E> transferTarget = null;
		boolean isLeft = false;
		if(left != null)
		{
			// Check this ones left neighbour to see if it is promoted
			if(left.LastPointers[0] != 0)
			{
				ItemContainer<E> temp = readAt(left.LastPointers[0]);
				if(temp.Levels == 1)
				{
					LogUtil.info("** Transfering levels to left neighbour");
					transferTarget = left;
					isLeft = true;
				}
			}
			else
			{
				// Its ok, its next to the edge
				LogUtil.info("** Transfering levels to left neighbour");
				transferTarget = left;
				isLeft = true;
			}
		}
		
		if(transferTarget == null && right != null)
		{
			// Check this ones right neighbour to see if it is promoted
			if(right.NextPointers[0] != 0)
			{
				ItemContainer<E> temp = readAt(right.NextPointers[0]);
				if(temp.Levels == 1)
				{
					LogUtil.info("** Transfering levels to right neighbour");
					transferTarget = right;
				}
			}
			else
			{
				// Its ok, its next to the edge
				LogUtil.info("** Transfering levels to right neighbour");
				transferTarget = right;
			}
		}
		
		if(transferTarget != null)
		{
			int oldSize = transferTarget.getSize();
			// Give it my levels and adjust references
			long last,next;
			last = transferTarget.LastPointers[0];
			next = transferTarget.NextPointers[0];
			transferTarget.LastPointers = Arrays.copyOf(container.LastPointers, container.Levels);
			transferTarget.NextPointers = Arrays.copyOf(container.NextPointers, container.Levels);
			transferTarget.LastWidths = Arrays.copyOf(container.LastWidths, container.Levels);
			transferTarget.NextWidths = Arrays.copyOf(container.NextWidths, container.Levels);
			
			if(isLeft)
				transferTarget.LastPointers[0] = last;
			else
				transferTarget.NextPointers[0] = next;
				
			transferTarget.Levels = container.Levels; 
			
			long targetLocation = (isLeft ? container.LastPointers[0] : container.NextPointers[0]);
			
			// Adjust references to this
			for(int l = container.Levels - 1; l >= 1; l--)
			{
				if(isLeft)
				{
					transferTarget.LastWidths[l]--;
					transferTarget.NextWidths[l]++;
				}
				else
				{
					transferTarget.LastWidths[l]++;
					transferTarget.NextWidths[l]--;
				}
				
				ItemContainer<E> temp = readAt(transferTarget.LastPointers[l]);
				if(temp != null)
				{
					temp.NextPointers[l] = targetLocation;
					temp.NextWidths[l] += (isLeft ? -1 : 1);
					writeAt(transferTarget.LastPointers[l], temp);
				}
				
				temp = readAt(transferTarget.NextPointers[l]);
				if(temp != null)
				{
					temp.LastPointers[l] = targetLocation;
					temp.LastWidths[l] += (isLeft ? 1 : -1);
					writeAt(transferTarget.NextPointers[l], temp);
				}
			}
			
			long newLocation = relocate(transferTarget, targetLocation, oldSize);
			
			// Make sure to update the location of the top element
			if(transferTarget.Levels == mHeader.LevelCount)
				mHeader.TopElement = newLocation;
			
			// Update info
			if(isLeft)
				container.LastPointers[0] = newLocation;
			else
				container.NextPointers[0] = newLocation;
			
			return true;
		}
		else
		{
			LogUtil.info("** Attempting to promote/demote all neighbours");
			
			long topMost = 0;
			int topLevels = 1;
			// Adjust references to this
			for(int l = container.Levels - 1; l >= 1; l--)
			{
				left = readAt(container.LastPointers[l]);
				right = readAt(container.NextPointers[l]);
				if(left != null)
				{
					if(topMost == 0)
					{
						topMost = container.LastPointers[l];
						topLevels = l+1;
					}
					
					left.NextPointers[l] = container.NextPointers[l];
					left.NextWidths[l] += container.NextWidths[l];
					writeAt(container.LastPointers[l], left);
				}
				
				if(right != null)
				{
					if(topMost == 0)
					{
						topMost = container.NextPointers[l];
						topLevels = l+1;
					}
					right.LastPointers[l] = container.LastPointers[l];
					right.LastWidths[l] += container.LastWidths[l];
					writeAt(container.NextPointers[l], right);
				}
			}
			
			if(container.Levels == mHeader.LevelCount && topMost != 0)
			{
				mHeader.TopElement = topMost;
				mHeader.LevelCount = topLevels;
			}
			
			// Try to promote / demote the neighbours at each level
			for(int l = container.Levels - 1; l >= 1; l--)
			{
				left = readAt(getModifiedAddress(container.LastPointers[l]));
				if(left != null)
				{
					if(!handlePromote(left,getModifiedAddress(container.LastPointers[l]),left.Levels-1,index - container.LastWidths[l]))
						handleDemote(left,getModifiedAddress(container.LastPointers[l]),left.Levels-1,index - container.LastWidths[l]);
				}
				
				right = readAt(getModifiedAddress(container.NextPointers[l]));
				
				if(right != null)
				{
					if(!handlePromote(right,getModifiedAddress(container.NextPointers[l]),right.Levels-1,index + container.NextWidths[l]))
						handleDemote(right,getModifiedAddress(container.NextPointers[l]),right.Levels-1,index + container.NextWidths[l]);
				}
			}
			
			
			
			return true;
		}
		
	}
	/**
	 * Gets the actual address of an item that was relocated
	 * @param knownAddr
	 * @return
	 */
	private long getModifiedAddress(long knownAddr)
	{
		long currentAddr = knownAddr;
		
		while(currentAddr != 0 && mAddressMappings.containsKey(currentAddr))
			currentAddr = mAddressMappings.get(currentAddr);
		
		return currentAddr;
	}
	private void clearModifiedAddresses()
	{
		mAddressMappings.clear();
	}
	
	private boolean handleDemote(ItemContainer<E> container, long location, int level, int index) throws IOException
	{
		// DEMOTE RULES:
		// Rule 1: If the 2 neighbours are promoted, the one with lower level is demoted.
        //  	   If they are the same level, one is randomly demoted to level 0

		// Rule 2: If I am promoted, try to move my levels to one of my neighbours
		//         Remember the constraint must NOT be violated
		//         If neather of them can accept the promotion, They will dissapear but, 
		//            Try to promote all the neighbours starting at the highest
		
		// Rule 3: If one is demoted, it should try to promote or demote its neighbours
		
		// Rule 4: If the top 2 next or last pointers are the same but not 0 (unless both next and last are 0), it should be demoted
		
		ItemContainer<E> left = null, right = null;
		
		if(container.Levels <= 1)
			return false;

		boolean leftZero = (container.LastPointers[container.Levels-1] == 0 && container.LastPointers[container.Levels-2] == 0);
		boolean rightZero = (container.NextPointers[container.Levels-1] == 0 && container.NextPointers[container.Levels-2] == 0);
		boolean bothLeft = (container.LastPointers[container.Levels-1] == container.LastPointers[container.Levels-2] );
		boolean bothRight = (container.NextPointers[container.Levels-1] == container.NextPointers[container.Levels-2] );
	
		if(((bothLeft && !leftZero) || (bothRight && !rightZero)) || (leftZero && rightZero))
		{
			LogUtil.info("** Demoting item " + container.Data.hashCode() + " @" + location + " from level " + level);
			
			int oldSize = container.getSize();
			
			left = readAt(container.LastPointers[level]);
			right = readAt(container.NextPointers[level]);
			
			// Bypass top level
			if(left != null)
			{
				left.NextPointers[level] = container.NextPointers[level];
				left.NextWidths[level] += container.NextWidths[level];
				writeAt(container.LastPointers[level] ,left);
			}
			if(right != null)
			{
				right.LastPointers[level] = container.LastPointers[level];
				right.LastWidths[level] += container.LastWidths[level];
				writeAt(container.NextPointers[level] ,right);
			}
			
			if(level == mHeader.LevelCount-1)
			{
				LogUtil.info("*** Decreasing Levels");
				mHeader.LevelCount--;
				// We will assume that this is still the top element
			}
			
			// Handle cascade demotes 
			if(left != null)
				handleDemote(left, container.LastPointers[level], left.Levels-1, index - container.LastWidths[level]);
			right = readAt(getModifiedAddress(container.NextPointers[level]));
			if(right != null)
				handleDemote(right, getModifiedAddress(container.NextPointers[level]), right.Levels-1, index + container.NextWidths[level]);
			
			// Actually demote it
			container.demote();
			
			onReliquishSpace(location + container.getSize(), oldSize - container.getSize());
			
			writeAt(location, container);
			
			
			
			level--;
			
			// Try to re-demote
			//handleDemote(container, location, level, index);


			// Handle cascade promotes of the next level down
			left = readAt(getModifiedAddress(container.LastPointers[level]));
			
			if(left != null)
				handlePromote(left,getModifiedAddress(container.LastPointers[level]),left.Levels-1,index - container.LastWidths[level]);

			right = readAt(getModifiedAddress(container.NextPointers[level]));
			if(right != null)
				handlePromote(right,getModifiedAddress(container.NextPointers[level]),right.Levels-1,index + container.NextWidths[level]);
			return true;
		}
		
		return false;
	}
	/**
	 * Handles removing the container, updating references, and promoting/demoting neighbours
	 */
	private void handleRemove(ItemContainer<E> container, long location, int index) throws IOException
	{
		LogUtil.info("* Removing item " + container.Data.hashCode() + " @ index " + index + " @" + location);
		ItemContainer<E> left = null, right = null;
		int size = container.getSize();
		
		// Get the left item
		left = readAt(container.LastPointers[0]);
		// Get the right item
		right = readAt(container.NextPointers[0]);
		
		// Bypass the bottom level
		if(left != null)
		{
			left.NextPointers[0] = container.NextPointers[0];
			left.NextWidths[0] = 1;
			writeAt(container.LastPointers[0], left);
		}
		
		if(right != null)
		{
			right.LastPointers[0] = container.LastPointers[0];
			right.LastWidths[0] = 1;
			writeAt(container.NextPointers[0], right);
		}
		
		// Both neighbours are promoted, so one of them must be totally demoted before removal
		if((left != null && right != null) && (left.Levels > 1 && right.Levels > 1))
		{
			if(left.Levels > right.Levels)
				doTotalDemote(right, container.NextPointers[0], index + container.NextWidths[0]);
			else
				doTotalDemote(left, container.LastPointers[0], index - container.LastWidths[0]);
		}
		else if(container.Levels != 1)
			transferLevelsToNeighbour(container, location, index);
		
		// and finally, adjust the widths above 
		container.Levels = 1;
		updateWidths(index, container, false);
		// And remove it
		onReliquishSpace(location, size);
		
		// Update the header
		mHeader.ItemCount--;
		if(index == 0)
			mHeader.LeftElement = container.NextPointers[0];
		if(index == mHeader.ItemCount)
			mHeader.RightElement = container.LastPointers[0];
		
		if(mHeader.ItemCount == 0)
		{
			mHeader.LevelCount = 0;
			mHeader.TopElement = 0;
		}
		clearModifiedAddresses();
		
		mFile.seek(mLocation);
		mHeader.write(mFile);
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) 
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
	public boolean retainAll(Collection<?> c) 
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() 
	{
		// TODO Auto-generated method stub
		
	}
	public E first()
	{
		try
		{
			if(mHeader.ItemCount == 0)
				throw new IndexOutOfBoundsException();
			
			return readAt(mHeader.LeftElement).Data;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	public E last()
	{
		try
		{
			if(mHeader.ItemCount == 0)
				throw new IndexOutOfBoundsException();
			
			return readAt(mHeader.RightElement).Data;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public Pair<Integer, E> get(Comparable<E> comparer)
	{
		try
		{
			if(mHeader.ItemCount == 0)
				throw new IndexOutOfBoundsException();
				
			int level = mHeader.LevelCount - 1;
			ItemContainer<E> container = readAt(mHeader.TopElement);
			int index = container.LastWidths[level];
			int dir = 0;
			while(level >= 0)
			{
				int res = comparer.compareTo(container.Data);
				if(res < 0)
				{
					if(dir == 1)
						return new Pair<Integer, E>(-1, null); // Cannot exist then
					
					if(container.LastPointers[level] == 0)
					{
						// Go down
						level--;
						dir = 0;
					}
					else
					{
						dir = -1;
						index -= container.LastWidths[level];
						container = readAt(container.LastPointers[level]);
					}
				}
				else if(res > 0)
				{
					if(dir == -1)
						return new Pair<Integer, E>(-1, null); // Cannot exist
					
					if(container.NextPointers[level] == 0)
					{
						// Go down
						level--;
						dir = 0;
					}
					else
					{
						dir = 1;
						index += container.NextWidths[level];
						container = readAt(container.NextPointers[level]);
					}
				}
				else
				{
					// Found an exact match
					return new Pair<Integer, E>(index, container.Data);
				}
			}
			
			return new Pair<Integer, E>(-1, null); // No matches
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return new Pair<Integer, E>(-1, null);
		}
	}
	@Override
	public E get(int index) 
	{
		try
		{
			// Bounds check
			if(index < 0 || index >= mHeader.ItemCount)
				throw new IndexOutOfBoundsException();
			
			int currentIndex = 0;
			int level = mHeader.LevelCount - 1;

			// Read the top item
			ItemContainer<E> container;
			container = readAt(mHeader.TopElement);
			currentIndex += container.LastWidths[level];
			
			// Find the right index
			int dir = 0;
			while(currentIndex != index && level >= 0)
			{
				if(index < currentIndex && dir != 1)
				{
					// Go to the last one
					if(container.LastPointers[level] == 0)
					{
						// Go down
						level--;
						dir = 0;
					}
					else
					{
						currentIndex -= container.LastWidths[level];
						
						mFile.seek(container.LastPointers[level]);
						container.read(mFile, mItemClass);
						dir = -1;
					}
				}
				else if(index > currentIndex && dir != -1)
				{
					// Go to the next one
					if(container.NextPointers[level] == 0)
					{
						// Go down
						level--;
						dir = 0;
					}
					else
					{
						currentIndex += container.NextWidths[level];
						
						mFile.seek(container.NextPointers[level]);
						container.read(mFile,mItemClass);
						dir = 1;
					}
				}
				else
				{
					// Go down
					level--;
					dir = 0;
				}
			}
			
			assert currentIndex == index : "Error in list structure";
			
			return container.Data;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public E set(int index, E element) 
	{
		try
		{
			// Bounds check
			if(index < 0 || index >= mHeader.ItemCount)
				throw new IndexOutOfBoundsException();
			
			int currentIndex = 0;
			int level = mHeader.LevelCount - 1;
			long location = mHeader.TopElement;
			
			// Read the top item
			ItemContainer<E> container;
			container = readAt(mHeader.TopElement);
			currentIndex += container.LastWidths[level];
			
			// Find the right index
			int dir = 0;
			while(currentIndex != index && level >= 0)
			{
				if(index < currentIndex && dir != 1)
				{
					// Go to the last one
					if(container.LastPointers[level] == 0)
					{
						// Go down
						level--;
						dir = 0;
					}
					else
					{
						currentIndex -= container.LastWidths[level];
						
						location = container.LastPointers[level];
						container = readAt(container.LastPointers[level]);
						
						dir = -1;
					}
				}
				else if(index > currentIndex && dir != -1)
				{
					// Go to the next one
					if(container.NextPointers[level] == 0)
					{
						// Go down
						level--;
						dir = 0;
					}
					else
					{
						currentIndex += container.NextWidths[level];
						
						location = container.NextPointers[level];
						container = readAt(container.NextPointers[level]);
						dir = 1;
					}
				}
				else
				{
					// Go down
					level--;
					dir = 0;
				}
			}
			
			assert currentIndex == index : "Error in list structure! Looking for " + index + " got " + currentIndex;
			
			int newHash = element.hashCode();
			E oldData = container.Data;
			if(newHash == container.Data.hashCode())
			{
				container.Data = element;
				writeAt(location, container);
				return oldData;
			}
			
			handleRemove(container, location, currentIndex);
			add(element);
			return oldData;
    	}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void add(int index, E element) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public E remove(int index) 
	{
		try
		{
			// Bounds check
			if(index < 0 || index >= mHeader.ItemCount)
				throw new IndexOutOfBoundsException();
			
			int currentIndex = 0;
			int level = mHeader.LevelCount - 1;
			long location = mHeader.TopElement;
			
			// Read the top item
			ItemContainer<E> container;
			container = readAt(mHeader.TopElement);
			currentIndex += container.LastWidths[level];
			
			// Find the right index
			int dir = 0;
			while(currentIndex != index && level >= 0)
			{
				if(index < currentIndex && dir != 1)
				{
					// Go to the last one
					if(container.LastPointers[level] == 0)
					{
						// Go down
						level--;
						dir = 0;
					}
					else
					{
						currentIndex -= container.LastWidths[level];
						
						location = container.LastPointers[level];
						container = readAt(container.LastPointers[level]);
						
						dir = -1;
					}
				}
				else if(index > currentIndex && dir != -1)
				{
					// Go to the next one
					if(container.NextPointers[level] == 0)
					{
						// Go down
						level--;
						dir = 0;
					}
					else
					{
						currentIndex += container.NextWidths[level];
						
						location = container.NextPointers[level];
						container = readAt(container.NextPointers[level]);
						dir = 1;
					}
				}
				else
				{
					// Go down
					level--;
					dir = 0;
				}
			}
			
			assert currentIndex == index : "Error in list structure! Looking for " + index + " got " + currentIndex;
			
			handleRemove(container, location, index);
			
			return container.Data;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public int indexOf(Comparable<E> comparer)
	{
		try
		{
			if(mHeader.ItemCount == 0)
				throw new IndexOutOfBoundsException();
				
			int level = mHeader.LevelCount - 1;
			int index = 0;
			ItemContainer<E> container = readAt(mHeader.TopElement);
			index += container.LastWidths[level];
			
			int dir = 0;
			while(level >= 0)
			{
				int res = comparer.compareTo(container.Data);
				if(res < 0)
				{
					if(dir == 1)
						return -1; // Cannot exist then
					
					if(container.LastPointers[level] == 0)
					{
						// Go down
						level--;
						dir = 0;
					}
					else
					{
						dir = -1;
						index -= container.LastWidths[level];
						container = readAt(container.LastPointers[level]);
					}
				}
				else if(res > 0)
				{
					if(dir == -1)
						return -1; // Cannot exist
					
					if(container.NextPointers[level] == 0)
					{
						// Go down
						level--;
						dir = 0;
					}
					else
					{
						dir = 1;
						index += container.NextWidths[level];
						container = readAt(container.NextPointers[level]);
					}
				}
				else
				{
					// Found an exact match
					return index;
				}
			}
			
			return -1; // No matches
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return -1;
		}
	}
	@Override
	public int indexOf(Object o) 
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int lastIndexOf(Object o) 
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ListIterator<E> listIterator() 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<E> listIterator(int index) 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	protected abstract long onRequestSpace(long size) throws IOException;
	protected abstract void onUseSpace(long location, long size) throws IOException;
	protected abstract void onReliquishSpace(long location, long size) throws IOException;
}

class ListHeader
{
	public static final int cSize = 32;
	public int LevelCount;
	public int ItemCount;
	
	public long TopElement;
	public long LeftElement;
	public long RightElement;
	
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeInt(LevelCount);
		file.writeInt(ItemCount);
		file.writeLong(TopElement);
		file.writeLong(LeftElement);
		file.writeLong(RightElement);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		LevelCount = file.readInt();
		ItemCount = file.readInt();
		TopElement = file.readLong();
		LeftElement = file.readLong();
		RightElement = file.readLong();
	}
}

class ItemContainer<E extends IWritable>
{
	public byte Levels;
	public long[] LastPointers;
	public long[] NextPointers;
	public int[] LastWidths;
	public int[] NextWidths;
	
	public E Data;
	
	public void promote()
	{
		LogUtil.info("**** Item " + Data.hashCode() + " was promoted to level " + Levels);
		Levels++;
		LastPointers = Arrays.copyOf(LastPointers, Levels);
		NextPointers = Arrays.copyOf(NextPointers, Levels);
		LastWidths = Arrays.copyOf(LastWidths, Levels);
		NextWidths = Arrays.copyOf(NextWidths, Levels);
	}
	public void demote()
	{
		if(Levels == 1)
			return;
		
		Levels--;
		LastPointers = Arrays.copyOf(LastPointers, Levels);
		NextPointers = Arrays.copyOf(NextPointers, Levels);
		LastWidths = Arrays.copyOf(LastWidths, Levels);
		NextWidths = Arrays.copyOf(NextWidths, Levels);
	}
	
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeByte(Levels);
		
		for(int i = 0; i < Levels; i++) 
			file.writeLong(LastPointers[i]);
		
		for(int i = 0; i < Levels; i++) 
			file.writeLong(NextPointers[i]);

		for(int i = 0; i < Levels; i++) 
			file.writeInt(LastWidths[i]);

		for(int i = 0; i < Levels; i++) 
			file.writeInt(NextWidths[i]);
		
		Data.write(file);
	}
	@SuppressWarnings("unchecked")
	public void read(RandomAccessFile file, Class<? extends IWritable> dataClass) throws IOException
	{
		Levels = file.readByte();
		LastPointers = new long[Levels];
		for(int i = 0; i < Levels; i++) 
			LastPointers[i] = file.readLong();
		
		NextPointers = new long[Levels];
		for(int i = 0; i < Levels; i++) 
			NextPointers[i] = file.readLong();
		LastWidths = new int[Levels];
		for(int i = 0; i < Levels; i++) 
			LastWidths[i] = file.readInt();
		NextWidths = new int[Levels];
		for(int i = 0; i < Levels; i++) 
			NextWidths[i] = file.readInt();
		
		try 
		{
			Data = (E) dataClass.newInstance();
			Data.read(file);
		} 
		catch (InstantiationException e) 
		{
			e.printStackTrace();
		} 
		catch (IllegalAccessException e) 
		{
			e.printStackTrace();
		}
	}
	
	public int getSize()
	{
		return 1 + (Levels * 8) * 2 + (Levels * 4) * 2 + Data.getSize();
	}
}
