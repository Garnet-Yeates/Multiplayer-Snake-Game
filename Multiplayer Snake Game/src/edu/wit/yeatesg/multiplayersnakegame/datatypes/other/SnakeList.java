package edu.wit.yeatesg.multiplayersnakegame.datatypes.other;

import java.lang.reflect.Field;
import java.util.ArrayList;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.ReflectionTools;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.SnakeUpdatePacket;

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
			SnakeData beingUpdated = get(updated.getClientName());
			set(indexOf(beingUpdated), updated);
			ArrayList<Field> fieldsToTransfer = ReflectionTools.getFieldsThatDontUpdate(beingUpdated.getClass());
			for (int i = 0; i < fieldsToTransfer.size(); i++)
			{       // Don't update the fields that start with '$', so after the SnakeData is updated in the list
				try // Set the updated '$' field values back to the non updated ones
				{
					Field f = fieldsToTransfer.get(i);
					f.setAccessible(true);
					Object beingUpdatedValue = f.get(beingUpdated);
					f.set(updated, beingUpdatedValue); 
				}
				catch (IllegalArgumentException | IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			add(updated);
		}
	}
}