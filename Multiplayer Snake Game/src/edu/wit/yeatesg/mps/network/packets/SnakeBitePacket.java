package edu.wit.yeatesg.mps.network.packets;

import edu.wit.yeatesg.mps.otherdatatypes.PointList;

public class SnakeBitePacket extends Packet
{
	private String biting;
	private String bitten;
	private PointList bitOff;
	
	public SnakeBitePacket(String... strings)
	{
		initFromStringArray(strings);
	}
	
	public SnakeBitePacket(String biting, String bitten, PointList bitOff)
	{
		this(biting.toString(), bitten.toString(), bitOff.toString());
	}
	
	public SnakeBitePacket(String splittable)
	{
		initFromStringArray(splittable.split(REGEX));
	}
	
	@Override
	protected void initFromStringArray(String[] args)
	{
		biting = args[0];
		bitten = args[1];
		bitOff = new PointList(args[2]);
	}
	
	public String getBiting()
	{
		return biting;
	}
	
	public void setBiting(String biting)
	{
		this.biting = biting;
	}
	
	public String getBitten()
	{
		return bitten;
	}
	
	public void setBitten(String bitten)
	{
		this.bitten = bitten;
	}
	
	public PointList getBitOff()
	{
		return bitOff;
	}
	
	public void setBitOff(PointList bitOff)
	{
		this.bitOff = bitOff;
	}
}