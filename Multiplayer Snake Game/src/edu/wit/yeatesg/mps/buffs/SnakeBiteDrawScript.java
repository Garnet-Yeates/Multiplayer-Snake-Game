package edu.wit.yeatesg.mps.buffs;

import java.awt.Graphics;
import edu.wit.yeatesg.mps.network.clientserver.GameplayGUI;
import edu.wit.yeatesg.mps.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.otherdatatypes.Snake;

public class SnakeBiteDrawScript extends DeadSnakeDrawScript
{
	public SnakeBiteDrawScript(GameplayGUI container, Snake who, PointList bitOff)
	{
		super(container, who, bitOff);
		velocityMultiplier = 0.012;
		start();
	}
	
	@Override
	public void drawSnake(Graphics g)
	{
		super.drawSnake(g);
		beingDrawn.drawSnakeNormally(g, drawingOn);
	}
}
