package edu.wit.yeatesg.mps.buffs;

import java.awt.Graphics;

import edu.wit.yeatesg.mps.network.clientserver.GameplayGUI;
import edu.wit.yeatesg.mps.otherdatatypes.Color;
import edu.wit.yeatesg.mps.otherdatatypes.Point;
import edu.wit.yeatesg.mps.otherdatatypes.SnakeData;

import static edu.wit.yeatesg.mps.network.clientserver.MultiplayerSnakeGame.*;

public class TranslucentBuffDrawScript extends SnakeDrawScript
{
	public TranslucentBuffDrawScript(GameplayGUI c, SnakeData who, int duration)
	{
		super(c, who, duration);
		start();
	}
	
	@Override
	public synchronized void drawSnake(Graphics graphics)
	{
		Color drawCol = getColorBasedOnRemainingBuffTime(beingDrawn.getColor(), Color.WHITE, 0.66666, 1);
		graphics.setColor(drawCol);

		int drawSize = UNIT_SIZE;
		
		int outlineThickness = getOutlineThicknessBasedOnProgress();

		for (Point p : beingDrawn.getPointList(true))
		{
			int drawX = GameplayGUI.getPixelCoord(p.getX());
			int drawY = GameplayGUI.getPixelCoord(p.getY());	
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
		int outlineThickness = (int) (thicknessMultiplier * MAX_OUTLINE_THICKNESS);
		outlineThickness = outlineThickness < MIN_THICKNESS ? MIN_THICKNESS : outlineThickness;
		return outlineThickness;
	}		
}

