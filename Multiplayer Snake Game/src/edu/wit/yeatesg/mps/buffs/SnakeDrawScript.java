package edu.wit.yeatesg.mps.buffs;


import java.awt.Graphics;

import javax.swing.Timer;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Color;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;

public abstract class SnakeDrawScript implements TickListener
{
	private GameplayClient drawingOn;

	protected SnakeData beingDrawn;

	protected int currentAnimationTick;
	protected int lastAnimationTick;

	private int animationRate;

	private long startTime;
	
	private long maxDuration;
	
	private AnimationTickTimer timer;
	
	public SnakeDrawScript(GameplayClient container, SnakeData who, long duration)
	{
		container.addTickListener(this);
		maxDuration = duration;
		drawingOn = container;
		beingDrawn = who;
		beingDrawn.setDrawScript(this);
		animationRate = 10;
		timer = new AnimationTickTimer();
	}
	
	protected final void start()
	{
		timer.start();
	}
	
	protected final void setTickRate(int tickRate)
	{
		this.animationRate = tickRate;
		timer.setDelay(animationRate);
	}

	public abstract void drawSnake(Graphics g);
	
	protected boolean justTicked = false;
	
	private class AnimationTickTimer extends Timer
	{
		private static final long serialVersionUID = 1530805279377680459L;

		public AnimationTickTimer()
		{
			super(animationRate, null);
			startTime = System.currentTimeMillis();
			addActionListener((e) ->
			{ 
				currentAnimationTick++;
				drawingOn.repaint();
				onAnimationTick();
				justTicked = true;
				if (System.currentTimeMillis() - startTime >= maxDuration)
				{
					beingDrawn.endBuffDrawScript();
					drawingOn.removeTickListener(SnakeDrawScript.this);
					stop();
				}
			});
		}
	}

	private boolean incrementing;

	private static final double minWhiteWeight = 0;
	private static final double maxWhiteWeight = 1;

	private static final double maxWhiteIncrement = 0.15;
	
	private double currentWhiteWeight = 0;
	
	protected boolean flashing = false;

	protected final Color getColorBasedOnRemainingBuffTime(Color normalCol, Color flashCol, double progressToFlashAt, int incrementMultiplier)
	{
		int r = normalCol.getRed(), g = normalCol.getGreen(), b = normalCol.getBlue();
		double progress = getProgress();
		
		if (progress > progressToFlashAt)
		{
			flashing = true;
			double flashProgress = (progress - progressToFlashAt) / (1 - progressToFlashAt);
			double whiteWeightIncrement = flashProgress * maxWhiteIncrement;
			currentWhiteWeight += incrementMultiplier * whiteWeightIncrement * (incrementing ? 1 : -1);
			
			if (currentWhiteWeight > maxWhiteWeight)
			{
				incrementing = false;
				currentWhiteWeight = maxWhiteWeight;
			}
			else if (currentWhiteWeight < minWhiteWeight)
			{
				incrementing = true;
				currentWhiteWeight = 0;
			}
			int flashR = flashCol.getRed(), flashG = flashCol.getGreen(), flashB = flashCol.getBlue();
			double flashWeight = currentWhiteWeight;
			double regularWeight =  1 - flashWeight;
			r = (int) (flashWeight*flashR + regularWeight*r);
			g = (int) (flashWeight*flashG + regularWeight*g);
			b = (int) (flashWeight*flashB + regularWeight*b);
			normalCol = new Color(r, g, b);
		}
		return normalCol;
	}
	
	public double getProgress()
	{
		return (double) (System.currentTimeMillis() - startTime) / (double) maxDuration;
	}
	
	public void onReceiveTick() { /* Subclasses can choose to implement this */ }
	
	protected void onAnimationTick() { /* Subclasses can choose to implement this */ }
}