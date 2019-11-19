package edu.wit.yeatesg.mps.network.packets;

import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;

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
