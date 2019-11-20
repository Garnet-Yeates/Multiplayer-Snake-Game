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

	protected int animationRate;

	private AnimationTickTimer animationTimer;

	private long startTime;
	private long endTime;
	private long duration;
	
	public BuffDrawScript(GameplayClient container, SnakeData whoGotTheBuff, int tickRate, int animationRate, int duration)
	{
		this.maxAnimationTicks = (int) (duration / animationRate);
		System.out.println("MAX ANIM TICKS: " + maxAnimationTicks);
		this.duration = duration;
		this.animationRate = animationRate;
		this.container = container;
		this.whoGotTheBuff = whoGotTheBuff;
		this.whoGotTheBuff.setBuffDrawScript(this);
		animationTimer = new AnimationTickTimer();
	}

	public abstract void drawSnake(Graphics g);

	
	private class AnimationTickTimer extends Timer
	{
		private static final long serialVersionUID = 1530805279377680459L;

		public AnimationTickTimer()
		{
			super(animationRate, null);
			addActionListener((e) ->
			{ 
				container.repaint();
				currentAnimationTick++;
				if (System.currentTimeMillis() - startTime >= duration)
				{
					whoGotTheBuff.endBuffDrawScript();
					stop();
					System.out.println("End time: " + (endTime = System.currentTimeMillis()));
					System.out.println("Duration: " + (duration = endTime - startTime));

				}
			});
			System.out.println("Start time: " + (startTime = System.currentTimeMillis()));
			start();
		}
	}

	private boolean flashingMore;

	protected final double defFlashIncDec = 0.02;
	protected final double maxFlashStrength = 1;

	protected double currFlashIncDec = 0;
	protected double currFlashStrength = 0;

	private double maxFlashIncDec = 0.15;

	protected final Color getColorBasedOnRemainingBuffTime(Color start, double progressToFlashAt, double flashIncMod)
	{
		int r = start.getRed(), g = start.getGreen(), b = start.getBlue();
		double progress = getProgress();
		
		if (progress > progressToFlashAt)
		{
			double flashProgress = (progress - progressToFlashAt) / (1 - progressToFlashAt);
			currFlashIncDec = flashProgress * maxFlashIncDec;
			currFlashStrength += currFlashIncDec * (flashingMore ? 1 : -1);
			
			if (currFlashStrength > maxFlashStrength)
			{
				flashingMore = false;
				currFlashStrength = maxFlashStrength;
			}
			else if (currFlashStrength < 0)
			{
				flashingMore = true;
				currFlashStrength = 0;
			}
			int flashR = 255, flashG = 255, flashB = 255;
			double flashWeight = currFlashStrength;
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