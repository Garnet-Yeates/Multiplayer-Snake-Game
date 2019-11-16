package edu.wit.yeatesg.multiplayersnakegame.datatypes.packet;

public class InitiateGamePacket extends Packet
{
	private int initialDelay = 0;
	private int totalDelay = 0;
	private int numCounts = 0;
	private int tickRate = 0;
	
	public InitiateGamePacket(String... paramsAsString)
	{
		super(paramsAsString);
		updateTickRate();
	}
	
	public InitiateGamePacket(int initialDelay, int totalDelay, int numCounts)
	{
		this(initialDelay + "", totalDelay + "", numCounts + "", "0");
	}
	
	public InitiateGamePacket(String splittable)
	{
		super(splittable.split(REGEX));
	}
	
	public int getNumCounts()
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

	@Override
	protected void initFromStringArray(String[] args)
	{
		numCounts = Integer.parseInt(args[0]);
		totalDelay = Integer.parseInt(args[1]);
		initialDelay = Integer.parseInt(args[2]);
	}
}
