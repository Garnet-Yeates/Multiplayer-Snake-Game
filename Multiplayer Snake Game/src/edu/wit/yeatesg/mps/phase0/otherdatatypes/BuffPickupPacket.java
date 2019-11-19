package edu.wit.yeatesg.mps.phase0.otherdatatypes;

import edu.wit.yeatesg.mps.network.packets.Packet;

public class BuffPickupPacket extends Packet
{
	private String buffName;
	
	public BuffPickupPacket(String... stringArgs)
	{
		super(stringArgs);
	}
	
	public BuffPickupPacket(String buffName)
	{
		this(new String[] { buffName } );
	}
	
	public String getBuffName()
	{
		return buffName;
	}
	
	public void setBuffName(String buffName)
	{
		this.buffName = buffName;
	}
	
	@Override
	protected void initFromStringArray(String[] args)
	{
		buffName = args[0];
	}
}
