package edu.wit.yeatesg.multiplayersnakegame.datatypes.packet;

public class TickPacket extends Packet
{
	private long currentTime;
	
	public TickPacket()
	{
		super(System.currentTimeMillis() + "");
	}
	
	public TickPacket(long currentTime)
	{
		this(currentTime + "");
	}
	
	public TickPacket(String splittableString)
	{
		super(splittableString);
	}
	
	@Override
	protected void initFromStringArray(String[] args)
	{		
		currentTime = Long.parseLong(args[0]);
	}
	
	public long getTime()
	{
		return currentTime;
	}
	
	public void setTime(long currentTime)
	{
		this.currentTime = currentTime;
	}
}
