package edu.wit.yeatesg.mps.network.packets;

import edu.wit.yeatesg.mps.otherdatatypes.Direction;

public class DirectionChangePacket extends Packet
{
	private String sender;
	private Direction newDirection;
	
	public DirectionChangePacket(String sender, Direction newDirection)
	{
		this.sender = sender;
		this.newDirection = newDirection;
	}
	
	public DirectionChangePacket(String splittable)
	{
		initFromStringArray(splittable.split(REGEX));
	}

	@Override
	protected void initFromStringArray(String[] args)
	{
		sender = args[0];
		newDirection = Direction.fromString(args[1]);
	}
	
	public String getSender()
	{
		return sender;
	}
	
	public void setSender(String sender)
	{
		this.sender = sender;
	}
	
	public Direction getDirection()
	{
		return newDirection;
	}
	
	public void setDirection(Direction direction)
	{
		newDirection = direction;
	}
}
