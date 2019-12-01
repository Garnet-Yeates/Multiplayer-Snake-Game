package edu.wit.yeatesg.mps.otherdatatypes;

public class DuplicateClientException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	@Override
	public String getMessage()
	{
		return "Name taken";
	}

}
