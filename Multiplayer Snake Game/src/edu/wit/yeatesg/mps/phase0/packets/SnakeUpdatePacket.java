package edu.wit.yeatesg.mps.phase0.packets;


import java.io.DataOutputStream;

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
		try {
			data = new SnakeData(args[0]);
		} catch (Exception e) {
			System.out.println("FAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAIL");
		}
	}
	
	public SnakeData getClientData()
	{
		return data;
	}
}
