package edu.wit.yeatesg.multiplayersnakegame.datatypes;

import java.util.ArrayList;

import edu.wit.yeatesg.multiplayersnakegame.packets.UpdateSingleClientPacket;

// No duplicate elements

public class ClientDataSet extends ArrayList<ClientData>
{
	public static final String REGEX = "/";

	private static final long serialVersionUID = -8714854719041332762L;

	public ClientDataSet(String... params)
	{
		for (String s : params)
		{
			ClientData data = new ClientData(s);
			if (!contains(data))
				add(data);
		}
	}

	public ClientDataSet(String splittableString)
	{
		this(splittableString.split(REGEX));
	}
	
	public int indexOf(String s)
	{
		for (ClientData data : this)
			if (data.getClientName().equalsIgnoreCase((String) s))
				return indexOf(data);
		return -1;
	}

	public boolean contains(String s)
	{
		for (ClientData data : this)
			if (data.getClientName().equalsIgnoreCase(s))
				return true;
		return false;
	}

	public ClientData get(String name)
	{
		int index = indexOf(name);
		return index != -1 ? get(index) : null;
	}

	@Override
	public String toString()
	{
		String s = "";
		int index = 0;
		for (ClientData dat : this)
		{
			s += dat + (index == size() - 1 ? "" : REGEX);
			index++;
		}
		return s;
	}
	
	public void updateBasedOn(UpdateSingleClientPacket pack)
	{
		ClientData updated = pack.getClientData();
		if (contains(updated.getClientName()))
		{
			ClientData updating = get(updated.getClientName());
			set(indexOf(updating), updated);
		}
		else
		{
			add(updated);
		}
	}
}