package edu.wit.yeatesg.mps.network.packets;

import edu.wit.yeatesg.mps.otherdatatypes.SnakeData;

public class SnakeBitePacket extends Packet
{
	private String biting;
	private String bitten;
	private int numSegmentsBit;
	private int interceptingIndex;
	
	public SnakeBitePacket(String... strings)
	{
		initFromStringArray(strings);
	}
	
	public SnakeBitePacket(String biting, String bitten, int numSegmentsBit, int interceptingIndex)
	{
		this(biting.toString(), bitten.toString(), numSegmentsBit + "", interceptingIndex + "");
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
		numSegmentsBit = Integer.parseInt(args[2]);
		interceptingIndex = Integer.parseInt(args[3]);
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
	
	public int getNumSegmentsBit()
	{
		return numSegmentsBit;
	}
	
	public void setNumSegmentsBit(int numSegmentsBit)
	{
		this.numSegmentsBit = numSegmentsBit;
	}
	
	public int getInterceptingIndex()
	{
		return interceptingIndex;
	}
	
	public void setInterceptingIndex(int interceptingIndex)
	{
		this.interceptingIndex = interceptingIndex;
	}
}