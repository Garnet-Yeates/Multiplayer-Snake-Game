package edu.wit.yeatesg.mps.phase0.otherdatatypes;

public class DuplicateClientException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	@Override
	public String getMessage()
	{
		return "Name taken";
	}

}
