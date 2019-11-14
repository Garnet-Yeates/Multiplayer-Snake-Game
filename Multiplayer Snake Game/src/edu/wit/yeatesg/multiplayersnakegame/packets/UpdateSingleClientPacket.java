package edu.wit.yeatesg.multiplayersnakegame.packets;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.ClientData;
 
public class UpdateSingleClientPacket extends Packet
{
	protected ClientData data;
	
	public UpdateSingleClientPacket(ClientData data)
	{
		super(data.toString());
	}
	
	public UpdateSingleClientPacket(String splittableString)
	{
		super(splittableString);
	}
	
	@Override
	protected void initFromStringArray(String[] args)
	{
		data = new ClientData(args[0]);
	}
	
	public ClientData getClientData()
	{
		return data;
	}
}
