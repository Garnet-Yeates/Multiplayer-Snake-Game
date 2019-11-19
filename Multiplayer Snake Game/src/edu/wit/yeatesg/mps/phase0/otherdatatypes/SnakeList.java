package edu.wit.yeatesg.mps.phase0.otherdatatypes;
import java.io.DataOutputStream;
import java.util.ArrayList;

import edu.wit.yeatesg.mps.phase0.packets.SnakeUpdatePacket;


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
	
	public int indexOf(String s)
	{
		for (SnakeData data : this)
			if (data.getClientName().equalsIgnoreCase((String) s))
				return indexOf(data);
		return -1;
	}

	public boolean contains(String s)
	{
		for (SnakeData data : this)
			if (data.getClientName().equalsIgnoreCase(s))
				return true;
		return false;
	}

	public SnakeData get(String name)
	{
		int index = indexOf(name);
		return index != -1 ? get(index) : null;
	}

	@Override
	public String toString()
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
	
	public void updateBasedOn(SnakeUpdatePacket pack)
	{
		SnakeData updated = pack.getClientData();
		if (contains(updated.getClientName()))
		{
			for (SnakeData data : this)
				if (data.getClientName().equals(updated.getClientName()))
					data.updateBasedOn(pack);
		}
		else
			add(updated);
	}
	
	
	// Server Methods
	
	
	public synchronized ArrayList<DataOutputStream> getAllOutputStreams()
	{
		ArrayList<DataOutputStream> list = new ArrayList<>();
		for (SnakeData dat : this)
			if (dat.getOutputStream() != null)
				list.add(dat.getOutputStream());
		return list;
	}
}