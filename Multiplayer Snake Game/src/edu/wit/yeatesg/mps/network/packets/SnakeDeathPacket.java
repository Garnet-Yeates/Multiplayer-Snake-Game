package edu.wit.yeatesg.mps.network.packets;

public class SnakeDeathPacket extends Packet
{
	private String snakeName;
	
	public SnakeDeathPacket(String snakeName)
	{
		initFromStringArray(new String[] { snakeName });
	}
	
	@Override
	protected void initFromStringArray(String[] args)
	{
		this.snakeName = args[0];
	}
	
	public String getSnakeName()
	{
		return snakeName;
	}
	
	public void setSnakeName(String snakeName)
	{
		this.snakeName = snakeName;
	}
}