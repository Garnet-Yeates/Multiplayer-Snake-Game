package edu.wit.yeatesg.mps.phase0.packets;


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
		try {
		sender = args[0];
		message = args[1];
		} catch (Exception e) {
			System.out.println("FAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAILFAIL");
		}
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