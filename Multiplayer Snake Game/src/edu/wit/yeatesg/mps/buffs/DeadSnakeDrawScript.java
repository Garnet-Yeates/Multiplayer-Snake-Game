package edu.wit.yeatesg.mps.buffs;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Color;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Point;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.PointList;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Vector;

public class DeadSnakeDrawScript extends SnakeDrawScript
{	
	protected LittleSegmentGroup[] respectiveSegmentGroups;
		
	public static final int DURATION = 1000;
	
	public DeadSnakeDrawScript(GameplayClient container, SnakeData who)
	{
		super(container, who, DURATION);
		System.out.println("new old fashioned dead dead");
		int index = 0;
		respectiveSegmentGroups = new LittleSegmentGroup[who.getLength()];
		for (Point p : who.getPointList(false))
		{
			LittleSegmentGroup group = new LittleSegmentGroup(p);
			respectiveSegmentGroups[index++] = group;	
		}
		velocityMultiplier = 1;
		start();
	}
	
	public DeadSnakeDrawScript(GameplayClient container, SnakeData who, PointList deadPoints)
	{
		super(container, who, DURATION);
		int index = 0;
		respectiveSegmentGroups = new LittleSegmentGroup[deadPoints.size()];
		for (Point p : deadPoints)
		{
			LittleSegmentGroup group = new LittleSegmentGroup(p);
			respectiveSegmentGroups[index++] = group;	
		}
	}

	@Override
	public synchronized void drawSnake(Graphics g)
	{
		for (int i = 0; i < respectiveSegmentGroups.length; respectiveSegmentGroups[i].draw(g.create()), i++);
	}
	
	@Override
	protected void onAnimationTick()
	{
		for (int i = 0; i < respectiveSegmentGroups.length; respectiveSegmentGroups[i].onTick(), i++);
	}
	
	protected double velocityMultiplier = 1.0;
	
	private class LittleSegmentGroup
	{
		private LittleSegment[] littleSegments = new LittleSegment[9];
		
		public LittleSegmentGroup(Point segmentLocation)
		{
			Point pixelCoords = GameplayClient.getPixelCoords(segmentLocation);
			int pixelX = pixelCoords.getX();
			int pixelY = pixelCoords.getY();

			double littleSegmentSize = (double) GameplayClient.UNIT_SIZE / 3;

			Point drawPoint = new Point(pixelX, pixelY);
			int origX = drawPoint.getX();
			int currVector = 0;
			
			Vector[] vecs = new Vector[]
			{ 
				new Vector(-1, -1), new Vector(0, -1), new Vector(1, -1),
				new Vector(-1, 0), new Vector(0, 0), new Vector(1, 0),
				new Vector(-1, 1), new Vector(0, 1), new Vector(1, 1)
			};
			
			int index = 0;
			for (int y = 0; y < 3; y++)
			{
				for (int x = 0; x < 3; x++)
				{
					littleSegments[index++] = new LittleSegment(vecs[currVector++], drawPoint.getX(), drawPoint.getY(), (int) littleSegmentSize);
					drawPoint.setX((int) (drawPoint.getX() + littleSegmentSize));
				}
				drawPoint.setY((int) (drawPoint.getY() + littleSegmentSize));
				drawPoint.setX(origX);
			}			
		}
		
		private void draw(Graphics g)
		{
			for (LittleSegment lilGuy : littleSegments)
				lilGuy.draw(g);
		}
		
		private void onTick()
		{
			for (LittleSegment lilGuy : littleSegments)
				lilGuy.onTick();
		}
		
		private class LittleSegment
		{
			private double drawX;
			private double drawY;
			private int size;
			private Color drawCol;
			private Vector direction;
			
			public LittleSegment(Vector vec, double drawX, double drawY, int size)
			{
				this.drawX = drawX;
				this.drawY = drawY;
				this.size = size;
				this.drawCol = beingDrawn.getColor();
				
				Random rand = new Random();
				double randAddX = rand.nextDouble() / 2;
				double randAddY = rand.nextDouble() / 2;
				vec = vec.add(new Vector(randAddX, randAddY));
				direction = vec.normalize();
			}
						
			public void onTick()
			{
//				y = opacity, x = getProgress() -> y = -x + 1
				double oM = -1.0, oB = 1.0;
				double opacity = oM*(getProgress()) + oB;
				double blackWeight = 1 - opacity;
				Color snakeCol = beingDrawn.getColor();
				Color blackCol = Color.BLACK;
				double snakeR = snakeCol.getRed(), snakeG = snakeCol.getGreen(), snakeB = snakeCol.getBlue();
				double blackR = blackCol.getRed(), blackG = blackCol.getGreen(), blackB = blackCol.getBlue();
				int r = (int) (blackWeight*blackR + opacity*snakeR), g = (int) (blackWeight*blackG + opacity*snakeG), b = (int) (blackWeight*blackB + opacity*snakeB);
				r = r > 255 ? 255 : r < 0 ? 0 : r;
				g = g > 255 ? 255 : g < 0 ? 0 : g;
				b = b > 255 ? 255 : b < 0 ? 0 : b;
				
				drawCol = new Color(r, g, b);
				
				int oldDrawX = (int) drawX;
				int oldDrawY = (int) drawY;
				drawX += (direction.getX()*5*velocityMultiplier);
				drawY += (direction.getY()*5*velocityMultiplier);	
				drawX = Double.isNaN(drawX) ? oldDrawX : drawX;
				drawY = Double.isNaN(drawY) ? oldDrawY : drawY;
			}
			
			public void draw(Graphics g)
			{
				int drawX = (int) this.drawX;
				int drawY = (int) this.drawY;

				g.setColor(drawCol);
				g.fillRect(drawX, drawY, size, size);
			}
		}
	}
}
