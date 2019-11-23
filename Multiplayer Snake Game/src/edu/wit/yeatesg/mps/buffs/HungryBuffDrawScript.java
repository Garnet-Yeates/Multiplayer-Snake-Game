package edu.wit.yeatesg.mps.buffs;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Color;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;

public class HungryBuffDrawScript extends SnakeDrawScript implements TickListener
{	
	private Color[] circleColor;

	public HungryBuffDrawScript(GameplayClient container, SnakeData who, long duration)
	{
		super(container, who, duration);
		setTickRate(20);

		circleColor = new Color[5];
		Color nextCol = Color.WHITE;
		Color originalSnakeColor = who.getColor();
		double thisColR = originalSnakeColor.getRed(), thisColG = originalSnakeColor.getGreen(), thisColB = originalSnakeColor.getBlue();
		double nextColR = nextCol.getRed(), nextColG = nextCol.getGreen(), nextColB = nextCol.getBlue();
		double rDiff = nextColR - thisColR, gDiff = nextColG - thisColG, bDiff = nextColB - thisColB;
		double rChangeRate = rDiff / 5, gChangeRate = gDiff / 5, bChangeRate = bDiff / 5;
		for (int i = 0; i < 5; i++)
		{
			circleColor[i] = new Color((int) thisColR, (int) thisColG, (int) thisColB);
			thisColR += rChangeRate; thisColG += gChangeRate; thisColB += bChangeRate;
		}
		start();
	}

	private int offset = 0;


	public static final int MAX_SHIFT_EVERY = 20;
	public static final int MIN_SHIFT_EVERY = 2;
	public static final double FLASH_AT = 0.7;
	
	@Override
	public synchronized void drawSnake(Graphics g)
	{	
		double progress = getProgress();
		double startRaisingShift = 0.7;
		int shiftWhiteEvery = MIN_SHIFT_EVERY;
		if (progress > startRaisingShift)
		{
			double shiftProgress = (progress - startRaisingShift) / (1 - startRaisingShift);
			shiftWhiteEvery = (int) (shiftProgress * MAX_SHIFT_EVERY);
			shiftWhiteEvery = shiftWhiteEvery < MIN_SHIFT_EVERY ? MIN_SHIFT_EVERY : shiftWhiteEvery;
		}
		
		if (currentAnimationTick % shiftWhiteEvery == 0)
			offset = (offset + 1) % circleColor.length;

		Point[] snakeLocs = beingDrawn.getPointList().toArray(new Point[0]);

		int numThreads = 4;
		Thread[] threads = new Thread[numThreads];
		ThreadTool[] tools = ThreadTool.splitIntoThreads(beingDrawn.getLength(), numThreads);
		int colIndex = offset;
		for (int i = 0; i < numThreads; i++)
		{
			ThreadTool tool = tools[i];
			if (i > 0)
				colIndex = (colIndex + (tools[i - 1].getEndIndex() - tools[i - 1].getStartIndex())) % circleColor.length;
			final int fColIndex = colIndex;
			final Graphics g2 = g.create();
			Thread t = new Thread(() ->
			{
				Iterator<Integer> it = tool.iterator();
				int cIndex = fColIndex;
				while (it.hasNext())
				{
					int sIndex = it.next();
					Color drawCol = circleColor[cIndex];
					cIndex = (cIndex + 1) % circleColor.length;
					Point drawPoint = GameplayClient.getPixelCoords(snakeLocs[sIndex]);
					int drawX = drawPoint.getX();
					int drawY = drawPoint.getY();
					int drawSize = GameplayClient.UNIT_SIZE;
					g2.setColor(drawCol);
					g2.fillRect(drawX, drawY, drawSize, drawSize);
				}
			});
			threads[i] = t;
			t.start();
		}	
		ThreadTool.waitForThreads(threads);
	}
		
	@Override
	public void onReceiveTick()
	{
		offset -= 1;
		if (offset < 0)
			offset = circleColor.length - 1;
	}
}	