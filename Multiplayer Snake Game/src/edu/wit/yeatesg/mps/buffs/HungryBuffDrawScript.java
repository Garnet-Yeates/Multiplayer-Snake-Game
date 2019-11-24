package edu.wit.yeatesg.mps.buffs;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Color;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeList;

public class HungryBuffDrawScript extends SnakeDrawScript implements TickListener
{	
	private Color[] snakeColorAssigner;
	public static final int MAX_SHIFT_EVERY = 20;
	public static final int MIN_SHIFT_EVERY = 2;
	private int snakeColAssignerOffset = 0;
	
	/** This will be true if the client who is connected to {@link SnakeDrawScript#drawingOn} is equal to {@link SnakeDrawScript#beingDrawn}*/
	private boolean highlightOthers;
	private Color[] highlightColorAssigner;
	public static final int HIGHLIGHT_ANIMATION_RATE = 8;
	private int highlightColIndex = 0;
	
	public HungryBuffDrawScript(GameplayClient container, SnakeData who, long duration)
	{
		super(container, who, duration);
		highlightOthers = drawingOn.getClient().equals(who);
		setTickRate(20);
		
//		The pattern of colors for the hungry snake is their color faded to white, and repeated
		snakeColorAssigner = Color.getBlendArray(who.getColor(), Color.WHITE, 5);
		
		if (highlightOthers)
		{
			for (SnakeData other : container.getOtherClients())
				other.setCurrentlyEatable(true);
			
//			This is for the flashing animation for when other Snakes' segments are highlighted
			Color[] whiteToBlack = Color.getBlendArray(Color.WHITE, Color.BLACK, 15);
			Color[] blackToWhite = Color.getBlendArray(Color.BLACK, Color.WHITE, 15);
			highlightColorAssigner = new Color[whiteToBlack.length + blackToWhite.length - 1];
			for (int i = 0; i < whiteToBlack.length; highlightColorAssigner[i] = whiteToBlack[i], i++);
			for (int i = 1; i < blackToWhite.length; highlightColorAssigner[whiteToBlack.length + i - 1] = blackToWhite[i], i++);
		}
		start();
	}
	
	@Override
	public synchronized void drawSnake(Graphics g)
	{	
		g = g.create();
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
			snakeColAssignerOffset = (snakeColAssignerOffset + 1) % snakeColorAssigner.length;
		
		if (highlightOthers)
			if (currentAnimationTick % HIGHLIGHT_ANIMATION_RATE == 0)
				highlightColIndex = (highlightColIndex + 1) % highlightColorAssigner.length;

		int numThreads = ThreadTool.getSuggestedNumThreads(beingDrawn.getLength());
		Thread[] threads = new Thread[numThreads];
		ThreadTool[] tools = ThreadTool.splitIntoThreads(beingDrawn.getLength());
		int colIndex = snakeColAssignerOffset;
		for (int i = 0; i < numThreads; i++)
		{
			ThreadTool tool = tools[i];
			if (i > 0) colIndex = i > 0 ? colIndex = (colIndex + (tools[i - 1].getEndIndex() - tools[i - 1].getStartIndex())) % snakeColorAssigner.length : colIndex;		
			final int fColIndex = colIndex;
			final Graphics g2 = g.create();
			Thread t = new Thread(() ->
			{
				Iterator<Integer> it = tool.iterator();
				int cIndex = fColIndex;
				while (it.hasNext())
				{
					int sIndex = it.next();
					Color drawCol = snakeColorAssigner[cIndex];
					cIndex = (cIndex + 1) % snakeColorAssigner.length;
					Point drawPoint = GameplayClient.getPixelCoords(beingDrawn.getPointList(false).get(sIndex));
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
		
		if (highlightOthers)
		{
			for (SnakeData otherClient : drawingOn.getOtherClients())
			{
				if (otherClient.getLength() >= Fruit.MIN_FRUIT_HUNGRY_LENGTH)
				{
					PointList otherPointList = otherClient.getPointList(true);
					for (int i = otherClient.getLength() - Fruit.MAX_BITE_OFF; i < otherClient.getLength(); i++)
					{
						Graphics g2 = g.create();
						g2.setColor(highlightColorAssigner[highlightColIndex]);
					//	System.out.println(currentAnimationTick + " -> " + g2.getColor().getRed() + "," + g2.getColor().getGreen() + "," + g2.getColor().getBlue());
						
						Point drawPoint = GameplayClient.getPixelCoords(otherPointList.get(i));
						int outlineThickness = 2;
						int drawSize = GameplayClient.UNIT_SIZE + outlineThickness*2;
						int drawX = drawPoint.getX() - outlineThickness; 
						int drawY = drawPoint.getY() - outlineThickness;	
						int offset = 0;
						for (int j = 0; j < outlineThickness; j++)
						{
							g2.drawRect(drawX + offset, drawY + offset, drawSize - 2*offset - 1, drawSize - 2*offset - 1);
							offset++;
						}
					}
				}
			}
		}
	}
		
	@Override
	public void onReceiveTick()
	{
		snakeColAssignerOffset -= 1;
		if (snakeColAssignerOffset < 0)
			snakeColAssignerOffset = snakeColorAssigner.length - 1;
	}
	
	@Override
	public void onEnd()
	{
		super.onEnd();
		if (highlightOthers)
			for (SnakeData dat : drawingOn.getOtherClients())
				dat.setCurrentlyEatable(false);
	}
}	