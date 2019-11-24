package edu.wit.yeatesg.mps.buffs;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;

public enum BuffType
{
	BUFF_TRANSLUCENT(10000), BUFF_HUNGRY(30000);

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
		if (this == BUFF_HUNGRY)
			new HungryBuffDrawScript(drawingOn, who, duration);
		else; // Other buffs here...
	}
	
	public static BuffType fromString(String s)
	{
		for (BuffType b : BuffType.values())
			if (s.equals(b.toString()))
				return b;
		return null;
	}	
}