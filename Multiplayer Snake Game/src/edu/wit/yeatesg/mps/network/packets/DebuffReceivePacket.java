package edu.wit.yeatesg.mps.network.packets;

import edu.wit.yeatesg.mps.buffs.BuffType;

public class DebuffReceivePacket extends Packet
{
	private String receiver;
	private BuffType buff;

	public DebuffReceivePacket(String... stringArgs)
	{
		super(stringArgs);
	}
	
	public DebuffReceivePacket(String splittable)
	{
		this(splittable.split(REGEX));
	}
	
	public DebuffReceivePacket(String sender, BuffType buff)
	{
		this(sender, buff.toString());
	}
	
	public BuffType getBuff()
	{
		return buff;
	}
	
	public void setBuff(BuffType buff)
	{
		this.buff = buff;
	}
	
	public String getReceiver()
	{
		return receiver;
	}
	
	public void setReceiver(String receiver)
	{
		this.receiver = receiver;
	}
	
	@Override
	protected void initFromStringArray(String[] args)
	{
		receiver = args[0];
		buff = BuffType.fromString(args[1]);
	}
}
