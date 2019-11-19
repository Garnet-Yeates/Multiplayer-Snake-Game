package edu.wit.yeatesg.mps.network.packets;


public class MessagePacket extends Packet
{
	protected String sender;
	protected String message;
	
	public MessagePacket(String sender, String message)
	{
		super(sender, message);
	}
	
	public MessagePacket(String splittableString)
	{
		super(splittableString);
	}

	@Override
	protected void initFromStringArray(String[] args)
	{
		sender = args[0];
		message = args[1];
	}
	
	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public String getSender()
	{
		return sender;
	}

	public void setSender(String sender)
	{
		this.sender = sender;
	}
}