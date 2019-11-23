package edu.wit.yeatesg.mps.buffs;

import java.awt.Graphics;
import java.util.ArrayList;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;

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
		beingDrawn.drawSnakeNormally(g);
		/*for (Point p : beingDrawn.getPointList())
		{
			Point drawPoint = GameplayClient.getPixelCoords(p);
			int drawSize = GameplayClient.UNIT_SIZE;
			g.setColor(beingDrawn.getColor());
			g.fillRect(drawPoint.getX(), drawPoint.getY(), drawSize, drawSize);
		}*/
	}

}
