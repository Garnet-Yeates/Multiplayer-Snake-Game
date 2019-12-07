package edu.wit.yeatesg.mps.otherdatatypes;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;


// No duplicate elements

public class SnakeList extends ArrayList<SnakeData>
{
	public static final String REGEX = "/";

	private static final long serialVersionUID = -8714854719041332762L;
	
	public SnakeList(String... params)
	{
		for (String s : params)
		{
			SnakeData data = new SnakeData(s);
			if (!contains(data))
				add(data);
		}
	}

	public SnakeList(String splittableString)
	{
		this(splittableString.split(REGEX));
	}
	
	public synchronized int indexOf(String s)
	{
		for (SnakeData data : this)
			if (data.getClientName().equalsIgnoreCase((String) s))
				return indexOf(data);
		return -1;
	}

	public synchronized boolean contains(String s)
	{
		for (SnakeData data : this)
			if (data.getClientName().equalsIgnoreCase(s))
				return true;
		return false;
	}

	public synchronized SnakeData get(String name)
	{
		int index = indexOf(name);
		return index != -1 ? get(index) : null;
	}

	@Override
	public synchronized String toString()
	{
		String s = "";
		int index = 0;
		for (SnakeData dat : this)
		{
			s += dat + (index == size() - 1 ? "" : REGEX);
			index++;
		}
		return s;
	}
	
	private SnakeData joinedOnLastUpdate = null;
	
	public synchronized void updateBasedOn(SnakeUpdatePacket pack)
	{
		SnakeData updated = pack.getClientData();
		if (contains(updated.getClientName()))
		{
			for (SnakeData data : this)
				if (data.getClientName().equals(updated.getClientName()))
					data.updateBasedOn(pack);
			joinedOnLastUpdate = null;
		}
		else
		{
			add(updated);
			joinedOnLastUpdate = updated;
		}
	}
	
	public synchronized boolean didSomeoneJoinOnLastUpdate()
	{
		return joinedOnLastUpdate != null;
	}
	
	public synchronized SnakeData getWhoJoinedOnLastUpdate()
	{
		return joinedOnLastUpdate;
	}
	
	public synchronized ArrayList<SnakeData> getAliveSnakes()
	{
		ArrayList<SnakeData> livingSnakes = new ArrayList<>();
		for (SnakeData snake : this)
			if (snake.isAlive())
				livingSnakes.add(snake);
		return livingSnakes;
	}
	
	public synchronized ArrayList<SnakeData> getAllSnakesExcept(SnakeData exluding)
	{
		ArrayList<SnakeData> otherSnakes = new ArrayList<>();
		for (SnakeData snake : this)
			if (!snake.equals(exluding))
				otherSnakes.add(snake);
		return otherSnakes;
	}
	
	
	// Server-Only Methods
	
	
	public synchronized ArrayList<BufferedOutputStream> getAllOutputStreams()
	{
		ArrayList<BufferedOutputStream> list = new ArrayList<>();
		for (SnakeData dat : this)
			if (dat.getOutputStream() != null)
				list.add(dat.getOutputStream());
		return list;
	}
	
	public synchronized SnakeList clone()
	{
		SnakeList clone = new SnakeList();
		clone.addAll(this);
		return clone;
	}
	
	// Unchanged except for adding synchronization
	
	@Override
	public synchronized boolean remove(Object o)
	{
		return super.remove(o);
	}
	
	@Override
	public synchronized SnakeData remove(int index)
	{
		return super.remove(index);
	}
	
	@Override
	public synchronized boolean add(SnakeData e)
	{
		return super.add(e);
	}
	
	@Override
	public synchronized void add(int index, SnakeData element)
	{
		super.add(index, element);
	}
	
	@Override
	public synchronized boolean addAll(Collection<? extends SnakeData> c)
	{
		return super.addAll(c);
	}
	
	@Override
	public synchronized Iterator<SnakeData> iterator()
	{
		return new SnakeListIterator();
	}
	
	public class SnakeListIterator implements Iterator<SnakeData>
	{
		private int cursor = 0;
		
		@Override
		public boolean hasNext()
		{
			synchronized (SnakeList.this)
			{
				return cursor < size();
			}	
		}

		@Override
		public SnakeData next()
		{
			synchronized (SnakeList.this)
			{
				return get(cursor++);
			}
		}
		
	}
}