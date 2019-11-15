package edu.wit.yeatesg.multiplayersnakegame.datatypes.packet;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeData;
 
public class SnakeUpdatePacket extends Packet
{
	protected SnakeData data;
	
	public SnakeUpdatePacket(SnakeData data)
	{
		super(data.toString());
	}
	
	public SnakeUpdatePacket(String splittableString)
	{
		super(splittableString);
	}
	
	@Override
	protected void initFromStringArray(String[] args)
	{
		data = new SnakeData(args[0]);
	}
	
	public SnakeData getClientData()
	{
		return data;
	}
}
