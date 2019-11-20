package edu.wit.yeatesg.mps.buffs;


import java.awt.Graphics;

import javax.swing.Timer;

import edu.wit.yeatesg.mps.network.clientserver.GameplayClient;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.Color;
import edu.wit.yeatesg.mps.phase0.otherdatatypes.SnakeData;

public abstract class BuffDrawScript
{
	private GameplayClient container;

	protected SnakeData whoGotTheBuff;

	protected int currentTick;
	protected int currentAnimationTick;
	protected int lastAnimationTick;

	protected int maxAnimationTicks;

	protected int animationRate = 10;

	private long startTime;
	private long endTime;
	private long duration;
	
	public BuffDrawScript(GameplayClient container, SnakeData whoGotTheBuff, int tickRate, int animationRate, int duration)
	{
		System.out.println("TRANS LUCY WOOCY");
		this.maxAnimationTicks = (int) (duration / animationRate);
		System.out.println("MAX ANIM TICKS: " + maxAnimationTicks);
		this.duration = duration;
		this.animationRate = animationRate;
		this.container = container;
		this.whoGotTheBuff = whoGotTheBuff;
		this.whoGotTheBuff.setBuffDrawScript(this);
		new AnimationTickTimer().start();
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
				container.repaint();
				currentAnimationTick++;
				if (System.currentTimeMillis() - startTime >= duration)
				{
					whoGotTheBuff.endBuffDrawScript();
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
		return (double) (System.currentTimeMillis() - startTime) / (double) duration;
	}
}