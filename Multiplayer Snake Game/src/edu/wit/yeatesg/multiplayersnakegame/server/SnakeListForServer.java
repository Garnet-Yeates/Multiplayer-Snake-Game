package edu.wit.yeatesg.multiplayersnakegame.server;

import java.io.DataOutputStream;
import java.util.ArrayList;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeList;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.packet.SnakeUpdatePacket;

public class SnakeListForServer extends SnakeList
{
	private static final long serialVersionUID = -460633197466993657L;

	@Override
	public boolean add(SnakeData e)
	{
		if (!(e instanceof SnakeDataForServer))
			throw new RuntimeException();
		else return super.add(e);
	}
	
	public ArrayList<DataOutputStream> getAllOutputStreams()
	{
		ArrayList<DataOutputStream> osList = new ArrayList<>();
		for (SnakeData data : this)
			osList.add(((SnakeDataForServer) data).getOutputStream());	
		return osList;
	}
	
	public synchronized void updateBasedOn(SnakeUpdatePacket pack)
	{
		SnakeDataForServer updatedInfo = new SnakeDataForServer(pack.getClientData());
		if (contains(updatedInfo.getClientName()))
		{
			SnakeDataForServer updating = (SnakeDataForServer) get(updatedInfo.getClientName());
			updatedInfo.setSocket(updating.getSocket());
			updatedInfo.setOutputStream(updating.getOutputStream());
			set(indexOf(updating), updatedInfo);
		}
		else
		{
			add(updatedInfo);
		}
	}
}
