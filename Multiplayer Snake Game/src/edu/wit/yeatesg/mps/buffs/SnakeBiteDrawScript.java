package edu.wit.yeatesg.mps.buffs;

import java.awt.Graphics;
import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeData;

public class SnakeBiteDrawScript extends DeadSnakeDrawScript
{
	public SnakeBiteDrawScript(GameplayClient container, SnakeData who, PointList bitOff)
	{
		super(container, who, bitOff);
		velocityMultiplier = 0.008;
		start();
	}
	
	@Override
	public synchronized void drawSnake(Graphics g)
	{
		super.drawSnake(g);
		beingDrawn.drawSnakeNormally(g, drawingOn);
	}

}
