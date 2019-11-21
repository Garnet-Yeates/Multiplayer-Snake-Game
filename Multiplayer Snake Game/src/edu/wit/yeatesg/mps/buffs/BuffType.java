package edu.wit.yeatesg.mps.buffs;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;

public enum BuffType
{
	BUFF_TRANSLUCENT(10000);

	private int duration;
	
	private BuffType(int duration)
	{
		this.duration = duration;
	}
	
	public int getDuration()
	{
		return duration;
	}
	
	public void startDrawScript(GameplayClient drawingOn, SnakeData who)
	{
		if (this == BUFF_TRANSLUCENT)
			new TranslucentBuffDrawScript(drawingOn, who, duration);
		else; // Other buffs here...
	}
	
	public static BuffType fromString(String s)
	{
		for (BuffType b : BuffType.values())
			if (s.equals(b.toString()))
				return b;
		return null;
	}
	
	// Make fruit be an enum as well, with a draw(g) method..
}
