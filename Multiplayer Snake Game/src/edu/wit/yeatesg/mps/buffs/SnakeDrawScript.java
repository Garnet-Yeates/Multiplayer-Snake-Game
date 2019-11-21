package edu.wit.yeatesg.mps.buffs;


import java.awt.Graphics;

import javax.swing.Timer;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Color;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;

public abstract class SnakeDrawScript
{
	private GameplayClient drawingOn;

	protected SnakeData beingDrawn;

	protected int currentTick;
	protected int currentAnimationTick;
	protected int lastAnimationTick;

	private int animationRate = 10;

	private long startTime;
	private long endTime;
	private long duration; // May be off from maxDuration by approximately +-1%
	
	private long maxDuration;
	
	private AnimationTickTimer timer;
	
	public SnakeDrawScript(GameplayClient container, SnakeData who, long duration)
	{
		this.maxDuration = duration;
		this.drawingOn = container;
		this.beingDrawn = who;
		this.beingDrawn.setDrawScript(this);
		timer = new AnimationTickTimer();
	}
	
	protected void start()
	{
		timer.start();
	}

	public abstract void drawSnake(Graphics g);
	
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
				onTick();
				if (System.currentTimeMillis() - startTime >= maxDuration)
				{
					beingDrawn.endBuffDrawScript();
					stop();
					endTime = System.currentTimeMillis();
					duration = endTime - startTime;
				}
			});
		}
	}

	private boolean incrementing;

	private static final double minWhiteWeight = 0;
	private static final double maxWhiteWeight = 1;

	private static final double maxWhiteIncrement = 0.15;
	
	private double currentWhiteWeight = 0;

	protected final Color getColorBasedOnRemainingBuffTime(Color start, double progressToFlashAt, double flashIncMod)
	{
		int r = start.getRed(), g = start.getGreen(), b = start.getBlue();
		double progress = getProgress();
		
		if (progress > progressToFlashAt)
		{
			double flashProgress = (progress - progressToFlashAt) / (1 - progressToFlashAt);
			double whiteWeightIncrement = flashProgress * maxWhiteIncrement;
			currentWhiteWeight += whiteWeightIncrement * (incrementing ? 1 : -1);
			
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
			int flashR = 255, flashG = 255, flashB = 255;
			double flashWeight = currentWhiteWeight;
			double regularWeight =  1 - flashWeight;
			r = (int) (flashWeight*flashR + regularWeight*r);
			g = (int) (flashWeight*flashG + regularWeight*g);
			b = (int) (flashWeight*flashB + regularWeight*b);
			start = new Color(r, g, b);
		}
		return start;
	}
	
	public double getProgress()
	{
		return (double) (System.currentTimeMillis() - startTime) / (double) maxDuration;
	}
	
	protected void onTick() { /* Subclasses can choose to implement this */ }
}