package au.com.mineauz.PlayerSpy.Utilities;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import au.com.mineauz.PlayerSpy.SpyPlugin;

public class AgeingMap<K, V> implements Map<K, V>
{
	private long mLife;
	private TreeMap<Long, List<K>> mAgeMap;
	private HashMap<K, Entry<Long, V>> mBackMap;
	
	private BukkitTask mTask;
	
	public AgeingMap(long life)
	{
		Validate.isTrue(life > 0);
		
		mLife = life;
		
		mAgeMap = new TreeMap<Long, List<K>>();
		mBackMap = new HashMap<K, Entry<Long,V>>();
	}

	@Override
	public V put( K key, V value )
	{
		long expireTime = System.currentTimeMillis() + mLife;
		
		if(mBackMap.containsKey(key))
		{
			long treeKey = mBackMap.get(key).getKey();
			
			List<K> keys = mAgeMap.get(treeKey);
			keys.remove(key);
			
			if(keys.isEmpty())
				mAgeMap.remove(treeKey);
		}
		
		mBackMap.put(key, new AbstractMap.SimpleEntry<Long, V>(expireTime, value));
		
		List<K> keys = mAgeMap.get(expireTime);
		if(keys == null)
		{
			keys = new ArrayList<K>();
			mAgeMap.put(expireTime, keys);
		}
		
		keys.add(key);
		
		if(mTask == null)
			mTask = Bukkit.getScheduler().runTaskLater(SpyPlugin.getInstance(), new CleanUpTask(), mLife / 50 + 1);
		
		return value;
	}
	
	public void renew(K key)
	{
		if(containsKey(key))
			put(key, get(key));
	}
	
	@Override
	public void putAll( Map<? extends K, ? extends V> m )
	{
		for(Entry<? extends K, ? extends V> entry : m.entrySet())
			put(entry.getKey(), entry.getValue());
	}
	
	@Override
	public void clear()
	{
		mAgeMap.clear();
		mBackMap.clear();
		
		if(mTask != null)
			mTask.cancel();
		mTask = null;
	}
	
	@Override
	public boolean containsKey( Object key )
	{
		return mBackMap.containsKey(key);
	}
	
	@Override
	public boolean containsValue( Object value )
	{
		for(Entry<Long, V> v : mBackMap.values())
		{
			if(v.getValue() == value || (v.getValue() != null && v.getValue().equals(value)))
				return true;
		}
		return false;
	}
	
	@Override
	public V get( Object key )
	{
		Entry<Long, V> value = mBackMap.get(key);
		if(value == null)
			return null;
		return value.getValue();
	}
	
	@Override
	public boolean isEmpty()
	{
		return mBackMap.isEmpty();
	}
	
	@Override
	public Set<Entry<K, V>> entrySet()
	{
		Set<Entry<K, V>> entries = new HashSet<Map.Entry<K,V>>();
		
		for(Entry<K, Entry<Long, V>> ent : mBackMap.entrySet())
			entries.add(new AbstractMap.SimpleEntry<K, V>(ent.getKey(), ent.getValue().getValue()));

		return Collections.unmodifiableSet(entries);
	}
	
	@Override
	public Set<K> keySet()
	{
		return mBackMap.keySet();
	}
	
	@Override
	public V remove( Object key )
	{
		if(mBackMap.containsKey(key))
		{
			long treeKey = mBackMap.get(key).getKey();
			
			List<K> keys = mAgeMap.get(treeKey);
			keys.remove(key);
			
			if(keys.isEmpty())
				mAgeMap.remove(treeKey);
			
			if(mAgeMap.isEmpty() && mTask != null)
			{
				mTask.cancel();
				mTask = null;
			}
			
			return mBackMap.remove(key).getValue();
		}
		
		return null;
	}
	
	@Override
	public int size()
	{
		return mBackMap.size();
	}
	
	@Override
	public Collection<V> values()
	{
		Set<V> vals = new HashSet<V>();
		
		for(Entry<Long, V> v : mBackMap.values())
			vals.add(v.getValue());

		return Collections.unmodifiableSet(vals);
	}
	
		
	private class CleanUpTask implements Runnable
	{
		@Override
		public void run()
		{
			while (!mAgeMap.isEmpty() && mAgeMap.firstKey() <= System.currentTimeMillis())
			{
				List<K> keys = mAgeMap.pollFirstEntry().getValue();
				
				for(K key : keys)
					mBackMap.remove(key);
			}
			
			if(mAgeMap.isEmpty())
			{
				mTask.cancel();
				mTask = null;
			}
			else
				mTask = Bukkit.getScheduler().runTaskLater(SpyPlugin.getInstance(), this, (mAgeMap.firstKey() - System.currentTimeMillis()) / 50 + 1);
		}
	}
}
