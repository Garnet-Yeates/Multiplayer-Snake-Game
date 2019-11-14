package edu.wit.yeatesg.multiplayersnakegame.packets;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.ClientData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.ClientDataSet;

public class UpdateAllClientsPacket extends Packet
{	
	protected ClientDataSet dataSet;
	
	public UpdateAllClientsPacket(ClientDataSet dataList)
	{
		super(dataList.toString());
	}
	
	public UpdateAllClientsPacket(String splittableString)
	{
		super(splittableString);
	}

	@Override
	protected void initFromStringArray(String[] args)
	{
		dataSet = new ClientDataSet(args[0]);
	}
	
	public ClientDataSet getDataList()
	{
		return dataSet;
	}
	
	public void setDataList(ClientDataSet newDataList)
	{
		dataSet = newDataList;
	}
}
