package edu.wit.yeatesg.mps.phase0.otherdatatypes;

import edu.wit.yeatesg.mps.phase0.packets.Packet;

public class InitiateGamePacket extends Packet
{
	private int initialDelay;
	private int totalDelay;
	private int numCounts;
	private int tickRate;
	
	public InitiateGamePacket(String... paramsAsString)
	{
		super(paramsAsString);
	}
	
	public InitiateGamePacket(int initialDelay, int totalDelay, int numCounts)
	{
		this(initialDelay + "", totalDelay + "", numCounts + "", "0");
	}
	
	public InitiateGamePacket(String splittable)
	{
		super(splittable.split(REGEX));
	}
	
	@Override
	protected void initFromStringArray(String[] args)
	{
		try {
			initialDelay = Integer.parseInt(args[0]);
			totalDelay = Integer.parseInt(args[1]);
			numCounts = Integer.parseInt(args[2]);
			tickRate = Integer.parseInt(args[3]);
		} catch (Exception e) {
				System.out.println("FAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAIL");
		}
		updateTickRate();
	}
	
	public int getNumTicks()
	{
		return numCounts;
	}
	
	public void setNumCounts(int newNumCounts)
	{
		this.numCounts = newNumCounts;
		if (numCounts != 0)
			updateTickRate();
	}
	
	public int getTotalDelay()
	{
		return totalDelay;
	}
	
	public void updateTickRate()
	{
		tickRate = (totalDelay - initialDelay) / numCounts;
	}
	
	public void setTotalDelay(int totalDelay)
	{
		this.totalDelay = totalDelay;
		if (numCounts != 0)
			updateTickRate();
	}
	
	public int getInitialDelay()
	{
		return initialDelay;
	}
	
	public void setInitialDelay(int initialDelay)
	{
		this.initialDelay = initialDelay;
		if (numCounts != 0)
			updateTickRate();
	}
	
	public int getTickRate()
	{
		return tickRate;
	}
}
