package edu.wit.yeatesg.mps.buffs;

import java.awt.Graphics;
import java.util.Iterator;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Color;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;

public class HungryBuffDrawScript extends SnakeDrawScript implements TickListener
{	
	private Color[] snakeColorAssigner;
	public static final int MAX_SHIFT_EVERY = 100;
	public static final int MIN_SHIFT_EVERY = 3;
	private int snakeColAssignerOffset = 0;
	
	/** This will be true if the client who is connected to {@link SnakeDrawScript#drawingOn} is equal to {@link SnakeDrawScript#beingDrawn}*/
	private boolean drawingSelf;
	private Color[] highlightColorAssigner;
	public static final int HIGHLIGHT_ANIMATION_RATE = 4;
	private int highlightColIndex = 0;
	
	public HungryBuffDrawScript(GameplayClient container, SnakeData who, long duration)
	{
		super(container, who, duration);
		drawingSelf = drawingOn.getClient().equals(who);
		setTickRate(20);
		
//		The pattern of colors for the hungry snake is their color faded to white, and repeated
		snakeColorAssigner = Color.getBlendArray(who.getColor(), Color.WHITE, 5);
		
		if (drawingSelf)
		{
//			This is for the flashing animation for when other Snakes' segments are highlighted
			Color[] whiteToBlack = Color.getBlendArray(Color.WHITE, Color.BLACK, 15);
			Color[] blackToWhite = Color.getBlendArray(Color.BLACK, Color.WHITE, 15);
			highlightColorAssigner = new Color[whiteToBlack.length + blackToWhite.length - 1];
			highlightOthers = true;
			for (int i = 0; i < whiteToBlack.length; highlightColorAssigner[i] = whiteToBlack[i], i++);
			for (int i = 1; i < blackToWhite.length; highlightColorAssigner[whiteToBlack.length + i - 1] = blackToWhite[i], i++);
		}
		start();
	}
	
	private boolean highlightOthers;
	
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
		
		if (drawingSelf)
			if (currentAnimationTick % HIGHLIGHT_ANIMATION_RATE == 0)
				highlightColIndex = (highlightColIndex + 1) % highlightColorAssigner.length;

//		Split this client's SnakeList into multiple threads if it is long enough according to ThreadTool, and draw the animation
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
					if (drawingSelf
						|| !drawingOn.getClient().hasHungryBuffDrawScript()
						|| sIndex == 0
						|| beingDrawn.getLength() > Fruit.MIN_FRUIT_HUNGRY_LENGTH && sIndex < beingDrawn.getLength() - Fruit.MAX_BITE_OFF)									
					{
						Color drawCol = snakeColorAssigner[cIndex];
						cIndex = (cIndex + 1) % snakeColorAssigner.length;
						Point drawPoint = GameplayClient.getPixelCoords(beingDrawn.getPointList(false).get(sIndex));
						int drawX = drawPoint.getX();
						int drawY = drawPoint.getY();
						int drawSize = GameplayClient.UNIT_SIZE;
						g2.setColor(drawCol);
						g2.fillRect(drawX, drawY, drawSize, drawSize);
					}
				}
			});
			threads[i] = t;
			t.start();
		}	
		ThreadTool.waitForThreads(threads);
		
//		If the client whose hungry buff animation is being drawn is also the client that is drawing it, display the edible locations on the other snakes
		if (drawingSelf)
		{
			for (SnakeData otherClient : drawingOn.getOtherClients())
			{
				if (otherClient.isAlive())
				{
					PointList otherPointList = otherClient.getPointList(true);
					int numIterations = 0;
					for (int i = otherClient.getLength() - 1; i != 0 && numIterations < Fruit.MAX_BITE_OFF; i--, numIterations++)
					{
						Graphics g2 = g.create();
						
//						If highlighting others, shrink the spots of the other Snakes that are edible
						g2.setColor(otherClient.getColor());
						Point drawPoint = GameplayClient.getPixelCoords(otherPointList.get(i));
						int shrink = 3;
						int drawX = drawPoint.getX() + shrink;
						int drawY = drawPoint.getY() + shrink;
						int drawSize = GameplayClient.UNIT_SIZE - 2*shrink;
						g2.fillRect(drawX, drawY, drawSize, drawSize);
						
//						Assign the highlight color. If the highlight color is black and the buff is about to end, stop displaying the highlight
						Color highlightCol = highlightColorAssigner[highlightColIndex];
						System.out.println(progress);
						if (progress > 0.80 && highlightCol.equals(Color.BLACK))
							highlightOthers = false;
						g2.setColor(highlightCol);

						if (highlightOthers)
						{
//							Draw the highlight
							int outlineThickness = 2;
							drawSize = GameplayClient.UNIT_SIZE;
							drawX = drawPoint.getX(); 
							drawY = drawPoint.getY();	
							int offset = 0; // Offset gets increased because it is drawing inwards
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
	}
		
	@Override
	public void onReceiveTick()
	{
		snakeColAssignerOffset -= 1;
		if (snakeColAssignerOffset < 0)
			snakeColAssignerOffset = snakeColorAssigner.length - 1;
	}
}	