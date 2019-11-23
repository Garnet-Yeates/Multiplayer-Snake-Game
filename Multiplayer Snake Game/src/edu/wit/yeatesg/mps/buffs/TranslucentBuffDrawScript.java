package edu.wit.yeatesg.mps.buffs;

import java.awt.Graphics;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Color;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;

public class TranslucentBuffDrawScript extends SnakeDrawScript
{
	public TranslucentBuffDrawScript(GameplayClient c, SnakeData who, int duration)
	{
		super(c, who, duration);
		start();
	}
	
	@Override
	public void drawSnake(Graphics graphics)
	{
		Color drawCol = getColorBasedOnRemainingBuffTime(beingDrawn.getColor(), Color.WHITE, 0.66666, 1);
		graphics.setColor(drawCol);

		int drawSize = GameplayClient.UNIT_SIZE;
		
		int outlineThickness = getOutlineThicknessBasedOnProgress();

		for (Point p : beingDrawn.getPointList())
		{
			int drawX = GameplayClient.getPixelCoord(p.getX());
			int drawY = GameplayClient.getPixelCoord(p.getY());	
			int offset = 0;

			for (int i = 0; i < outlineThickness; i++)
			{
				graphics.drawRect(drawX + offset, drawY + offset, drawSize - 2*offset - 1, drawSize - 2*offset - 1);
				offset++;
			}
		}
	}
	
	public static final int MIN_THICKNESS = 2;

	private int getOutlineThicknessBasedOnProgress()
	{
		// y= 4.000x^2 −6x + 1.000 where x = progress()
		double a = 8, b = -8, c = 1;
		double progress = getProgress();
		double thicknessMultiplier = a*Math.pow(progress, 2) + b*progress + c;
		int outlineThickness = (int) (thicknessMultiplier * GameplayClient.MAX_OUTLINE_THICKNESS);
		outlineThickness = outlineThickness < MIN_THICKNESS ? MIN_THICKNESS : outlineThickness;
		return outlineThickness;
	}		
}

