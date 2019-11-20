package edu.wit.yeatesg.mps.buffs;

import java.awt.Graphics;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Color;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;

public class TranslucentBuffDrawScript extends BuffDrawScript
{
	public TranslucentBuffDrawScript(GameplayClient c, SnakeData who, int duration)
	{
		super(c, who, 100, 10, duration);
	}
	
	@Override
	public void drawSnake(Graphics graphics)
	{
		Color drawCol = getColorBasedOnRemainingBuffTime(whoGotTheBuff.getColor(), 0.666, 10);
		graphics.setColor(drawCol);

		int drawSize = GameplayClient.UNIT_SIZE;
		
		double maxOutlineThickness = drawSize / 2;
		int outlineThickness = getOutlineThicknessBasedOnProgress(maxOutlineThickness);

		for (Point p : whoGotTheBuff.getPointList())
		{
			int drawX = GameplayClient.getPixelCoord(p.getX());
			int drawY = GameplayClient.getPixelCoord(p.getY());	
			int offset = 0;

			for (int i = 0; i < outlineThickness; i++)
			{
				graphics.drawRect(drawX + offset, drawY + offset, drawSize - 2*offset, drawSize - 2*offset);
				offset++;
			}
		}
	}

	private int getOutlineThicknessBasedOnProgress(double maxOutlineThickness)
	{
		// y= 4.000x^2 −6x + 1.000 -> x = progress()
		double a = 8, b = -8, c = 1;
		double progress = getProgress();
		double thicknessMultiplier = a*Math.pow(progress, 2) + b*progress + c;
		int outlineThickness = (int) (thicknessMultiplier * maxOutlineThickness);
		outlineThickness = outlineThickness < 2 ? 2 : outlineThickness;
		return outlineThickness;
	}		
}

