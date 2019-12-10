package edu.wit.yeatesg.mps.network.packets;

import edu.wit.yeatesg.mps.otherdatatypes.Snake;

public class SnakeUpdatePacket extends Packet
{
	protected Snake data;

	public SnakeUpdatePacket(Snake data)
	{
		super(data.toString());
		/*
		SnakeData clone = new SnakeData();
		clone.setPointList(null);
		clone.setIsAlive(data.isAlive());
		clone.setColor(data.getColor());
		clone.setDirection(data.getDirection());
		clone.setIsHost(data.isHost());
		clone.setIsAlive(data.isAlive());
		clone.setAddingSegment(data.isAddingSegment());*/
	}

	public SnakeUpdatePacket(String splittableString)
	{
		super(splittableString);
	}

	@Override
	protected void initFromStringArray(String[] args)
	{
		data = new Snake(args[0]);
	}

	public Snake getClientData()
	{
		return data;
	}
}
