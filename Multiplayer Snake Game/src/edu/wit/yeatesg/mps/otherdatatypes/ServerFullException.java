package edu.wit.yeatesg.mps.otherdatatypes;

public class ServerFullException extends RuntimeException
{
	private static final long serialVersionUID = 4886491942818103371L;

	@Override
	public String getMessage() {
		return "This server is full";
	}
}
