package edu.wit.yeatesg.multiplayersnakegame.datatypes;

import java.util.ArrayList;

import edu.wit.yeatesg.multiplayersnakegame.packets.UpdateAllClientsPacket;
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
	
	/**
	 * This is used by servers only, because servers receive updates from one client at a time
	 * @param pack
	 */
	public void updateBasedOn(UpdateSingleClientPacket pack)
	{
		ClientData updated = pack.getClientData();
		ClientData updating = get(updated.getClientName());
		set(indexOf(updating), updated);
	}
	
	private ClientData whoLeft;
	private ClientData whoJoined;
	
	/**
	 * This is used by clients only, because clients receive updates for all other connected clients at a time
	 * @param pack
	 */
	public void updateBasedOn(UpdateAllClientsPacket pack)
	{
		whoLeft = null;
		whoJoined = null;
		ClientDataSet receivedDataSet = pack.getDataList();
		if (receivedDataSet.size() < size()) // Someone left
		{
			ArrayList<ClientData> toRemove = new ArrayList<ClientData>();
			for (ClientData data : this)
			{
				if (!receivedDataSet.contains(data.getClientName()))
				{
					whoLeft = data;
					toRemove.add(whoLeft);
				}
			}
			for (ClientData dat : toRemove)
				remove(dat);
		}
		else if (receivedDataSet.size() > size()) // Someone joined
		{
			for (ClientData receivedData : receivedDataSet)
			{
				if (!this.contains(receivedData))
				{
					whoJoined = receivedData;
					add(whoJoined);
				}
			}
		}
		else // Regular update
			for (ClientData data : receivedDataSet)
				set(indexOf(data.getClientName()), data);
	}
	
	public boolean didSomeoneLeaveOnLastUpdate()
	{
		return whoLeft != null;
	}
	
	public ClientData getWhoLeftOnLastUpdate()
	{
		return whoLeft;
	}
	
	public boolean didSomeoneJoinOnLastUpdate()
	{
		return whoJoined != null;
	}
	
	public ClientData getWhoJoinedOnLastUpdate()
	{
		return whoJoined;
	}
}
