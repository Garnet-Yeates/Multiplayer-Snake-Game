package edu.wit.yeatesg.multiplayersnakegame.datatypes.packet;

public class ErrorPacket extends Packet
{
	protected String errorMessage;
	
	public ErrorPacket(String errorMessage)
	{
		super(errorMessage);
	}
	
	@Override
	protected void initFromStringArray(String[] args)
	{
		errorMessage = args[0];
	}
	
	public String getErrorMessage()
	{
		return errorMessage;
	}
	
	@Override
	public String toString()
	{
		return errorMessage;
	}
	
	public static ErrorPacket fromString(String packetAsString)
	{
		return new ErrorPacket(packetAsString);
	} 
}