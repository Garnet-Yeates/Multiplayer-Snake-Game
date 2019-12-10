package edu.wit.yeatesg.mps.otherdatatypes;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import edu.wit.yeatesg.mps.network.packets.SnakeUpdatePacket;


// No duplicate elements

public class SnakeList extends ArrayList<Snake>
{
	public static final String REGEX = "/";

	private static final long serialVersionUID = -8714854719041332762L;
	
	public SnakeList(String... params)
	{
		for (String s : params)
		{
			Snake data = new Snake(s);
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
		for (Snake data : this)
			if (data.getClientName().equalsIgnoreCase((String) s))
				return indexOf(data);
		return -1;
	}

	public synchronized boolean contains(String s)
	{
		for (Snake data : this)
			if (data.getClientName().equalsIgnoreCase(s))
				return true;
		return false;
	}

	public synchronized Snake get(String name)
	{
		int index = indexOf(name);
		return index != -1 ? get(index) : null;
	}

	@Override
	public synchronized String toString()
	{
		String s = "";
		int index = 0;
		for (Snake dat : this)
		{
			s += dat + (index == size() - 1 ? "" : REGEX);
			index++;
		}
		return s;
	}
	
	private Snake joinedOnLastUpdate = null;
	
	public synchronized void updateBasedOn(SnakeUpdatePacket pack)
	{
		Snake updated = pack.getClientData();
		if (contains(updated.getClientName()))
		{
			for (Snake data : this)
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
	
	public synchronized Snake getWhoJoinedOnLastUpdate()
	{
		return joinedOnLastUpdate;
	}
	
	public synchronized ArrayList<Snake> getAliveSnakes()
	{
		ArrayList<Snake> livingSnakes = new ArrayList<>();
		for (Snake snake : this)
			if (snake.isAlive())
				livingSnakes.add(snake);
		return livingSnakes;
	}
	
	public synchronized ArrayList<Snake> getAllSnakesExcept(Snake exluding)
	{
		ArrayList<Snake> otherSnakes = new ArrayList<>();
		for (Snake snake : this)
			if (!snake.equals(exluding))
				otherSnakes.add(snake);
		return otherSnakes;
	}
	
	
	// Server-Only Methods
	
	
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
	public synchronized Snake remove(int index)
	{
		return super.remove(index);
	}
	
	@Override
	public synchronized boolean add(Snake e)
	{
		return super.add(e);
	}
	
	@Override
	public synchronized void add(int index, Snake element)
	{
		super.add(index, element);
	}
	
	@Override
	public synchronized boolean addAll(Collection<? extends Snake> c)
	{
		return super.addAll(c);
	}
	
	@Override
	public synchronized Iterator<Snake> iterator()
	{
		return new SnakeListIterator();
	}
	
	public class SnakeListIterator implements Iterator<Snake>
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
		public Snake next()
		{
			synchronized (SnakeList.this)
			{
				return get(cursor++);
			}
		}
		
	}
}